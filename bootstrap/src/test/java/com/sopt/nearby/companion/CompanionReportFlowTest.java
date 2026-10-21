// 동행 단계별 신고와 매칭 기준 중복 방지를 HTTP·트랜잭션·DB 경계에서 검증한다.
package com.sopt.nearby.companion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sopt.nearby.companion.application.CompanionReportMailFormatter;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.companion.port.out.CompanionReportMail;
import com.sopt.nearby.companion.port.out.CompanionReportMailSender;
import com.sopt.nearby.companion.port.out.CompanionReportReasonRepository;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:report242;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "management.health.redis.enabled=false"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class CompanionReportFlowTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 12, 0);
    @Autowired private MockMvc mvc;
    @Autowired private DataSource dataSource;
    @Autowired private UserAccountRepository users;
    @Autowired private CompanionProfileRepository profiles;
    @MockitoBean private CompanionReportMailSender mailSender;
    @MockitoSpyBean private CompanionReportReasonRepository reportReasons;

    @ParameterizedTest
    @EnumSource(Stage.class)
    void allowsReportsThroughoutCompanionLifecycle(final Stage stage) throws Exception {
        int reports = 0;
        for (boolean byMatch : List.of(true, false)) {
            if (!byMatch && stage == Stage.SCHEDULING) {
                continue;
            }
            Ids ids = fixture(stage);
            report(ids, byMatch, ids.hostId(), body(ids.guestId()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("CREATE_COMPANION_REPORT"))
                    .andExpect(jsonPath("$.data.reportId").isNumber())
                    .andExpect(jsonPath("$.data.reportedAt").isString())
                    .andExpect(jsonPath(byMatch ? "$.data.matchId" : "$.data.meetingId")
                            .value(byMatch ? ids.matchId() : ids.meetingId()))
                    .andExpect(jsonPath(byMatch ? "$.data.meetingId" : "$.data.matchId").doesNotExist());
            assertThat(count("select count(*) from companion_report where match_id = ?", ids.matchId())).isEqualTo(1);
            assertThat(count("""
                    select count(*) from companion_report_reason rr join companion_report r on r.id = rr.report_id
                    where r.match_id = ?
                    """, ids.matchId())).isEqualTo(1);
            reports++;
        }
        ArgumentCaptor<CompanionReportMail> sent = ArgumentCaptor.forClass(CompanionReportMail.class);
        verify(mailSender, times(reports)).send(sent.capture());
        assertThat(sent.getAllValues()).allSatisfy(mail -> {
            assertThat(mail.placeAddress()).isEqualTo("Paris, France");
            assertThat(mail.companionPostContent()).isEqualTo("동행 본문");
            if (stage == Stage.SCHEDULING) {
                assertThat(mail.report().meetingId()).isNull();
                assertThat(mail.scheduledAt()).isNull();
                assertThat(CompanionReportMailFormatter.body(mail)).contains("일정 미확정");
            } else {
                assertThat(mail.scheduledAt()).isEqualTo(NOW);
            }
        });
    }

    @Test
    void persistsReportBeforeSendingMailAndBlocksDuplicateAfterScheduleConfirmation() throws Exception {
        Ids pending = fixture(Stage.SCHEDULING);
        doAnswer(invocation -> {
            CompanionReportMail mail = invocation.getArgument(0);
            assertThat(count("select count(*) from companion_report where id = ?", mail.report().id())).isEqualTo(1);
            return null;
        }).when(mailSender).send(any());
        report(pending, true, pending.hostId(), body(pending.guestId())).andExpect(status().isCreated());
        Ids confirmed = confirm(pending, Stage.BEFORE_CHECK_IN);
        for (boolean byMatch : List.of(true, false)) {
            report(confirmed, byMatch, confirmed.hostId(), body(confirmed.guestId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("COMPANION_REPORT_ALREADY_EXISTS"));
        }
        verify(mailSender, times(1)).send(any());
        assertThat(count("select count(*) from companion_report where match_id = ? and meeting_id is null",
                pending.matchId())).isEqualTo(1);
    }

    @Test
    void concurrentRequestsThroughBothApisCreateOneReportAndSendOneMail() throws Exception {
        Ids ids = fixture(Stage.ONGOING);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var requests = List.of(true, false).stream().map(byMatch -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("동시 신고 시작 대기 시간 초과");
                }
                return report(ids, byMatch, ids.hostId(), body(ids.guestId())).andReturn().getResponse().getStatus();
            })).toList();
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            assertThat(List.of(requests.get(0).get(10, TimeUnit.SECONDS), requests.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(count("select count(*) from companion_report where match_id = ?", ids.matchId())).isEqualTo(1);
        verify(mailSender, times(1)).send(any());
    }

    @Test
    void protectsMembershipTargetAndRequestValidationThroughBothApis() throws Exception {
        Ids ids = fixture(Stage.BEFORE_CHECK_IN);
        long outsider = user();
        for (boolean byMatch : List.of(true, false)) {
            report(ids, byMatch, outsider, body(ids.guestId())).andExpect(status().isForbidden());
            report(ids, byMatch, ids.hostId(), body(outsider)).andExpect(status().isNotFound());
            report(ids, byMatch, ids.hostId(), body(ids.hostId())).andExpect(status().isBadRequest());
            report(ids, byMatch, ids.hostId(), "{}").andExpect(status().isBadRequest());
            report(ids, byMatch, ids.hostId(), body(ids.guestId()).replace("NO_SHOW", "UNKNOWN"))
                    .andExpect(status().isBadRequest());
            report(ids, byMatch, ids.hostId(), body(ids.guestId()).replace("NO_SHOW", "ETC"))
                    .andExpect(status().isBadRequest());
        }
        assertThat(count("select count(*) from companion_report where match_id = ?", ids.matchId())).isZero();
        verifyNoInteractions(mailSender);
    }

    @Test
    void rejectsCanceledMatchAndMeetingThroughBothApis() throws Exception {
        Ids pending = fixture(Stage.SCHEDULING);
        sql("update companion_match set status = 'CANCELED' where id = ?", pending.matchId());
        report(pending, true, pending.hostId(), body(pending.guestId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMPANION_REPORT_ALREADY_CANCELED"));
        Ids confirmed = fixture(Stage.BEFORE_CHECK_IN);
        sql("update companion_meeting set status = 'CANCELED' where id = ?", confirmed.meetingId());
        for (boolean byMatch : List.of(true, false)) {
            report(confirmed, byMatch, confirmed.hostId(), body(confirmed.guestId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMPANION_REPORT_ALREADY_CANCELED"));
        }
        verifyNoInteractions(mailSender);
    }

    @Test
    void preservesHostNicknameReportingBeforeScheduleConfirmation() throws Exception {
        Ids ids = fixture(Stage.SCHEDULING);
        report(ids, true, ids.hostId(), """
                {"reasons":["NO_SHOW"],"detail":"사용자%s"}
                """.formatted(ids.guestId())).andExpect(status().isCreated());
        assertThat(count("select count(*) from companion_report where match_id = ? and reported_user_id = ?",
                ids.matchId(), ids.guestId())).isEqualTo(1);
    }

    @Test
    void rollsBackReportAndDoesNotSendMailWhenReasonPersistenceFails() throws Exception {
        Ids ids = fixture(Stage.SCHEDULING);
        doThrow(new IllegalStateException("신고 사유 저장 실패")).when(reportReasons).save(any());
        report(ids, true, ids.hostId(), body(ids.guestId())).andExpect(status().isInternalServerError());
        assertThat(count("select count(*) from companion_report where match_id = ?", ids.matchId())).isZero();
        verifyNoInteractions(mailSender);
    }

    @Test
    void fallsBackToPostTimeBeforeScheduleConfirmation() throws Exception {
        Ids ids = fixture(Stage.SCHEDULING);
        sql("""
                update companion_post set meeting_at = ?, meeting_time_type = 'SCHEDULED'
                where id = (select post_id from companion_match where id = ?)
                """, NOW, ids.matchId());
        report(ids, true, ids.hostId(), body(ids.guestId())).andExpect(status().isCreated());
        ArgumentCaptor<CompanionReportMail> sent = ArgumentCaptor.forClass(CompanionReportMail.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().scheduledAt()).isEqualTo(NOW);
        assertThat(sent.getValue().placeAddress()).isEqualTo("Paris, France");
        assertThat(sent.getValue().report().meetingId()).isNull();
    }

    @Test
    void documentsBothApisAndRemovesObsoleteRestrictions() throws Exception {
        String docs = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(docs).contains("/api/companion-matches/{matchId}/reports",
                "/api/companion-meetings/{meetingId}/reports", "COMPANION_REPORT_ALREADY_CANCELED")
                .doesNotContain("COMPANION_REPORT_CURRENT_USER_NOT_CHECKED_IN",
                        "COMPANION_REPORT_CURRENT_USER_ALREADY_COMPLETED", "COMPANION_REPORT_MEETING_NOT_ONGOING");
    }

    private ResultActions report(final Ids ids, final boolean byMatch, final long userId, final String body)
            throws Exception {
        String path = byMatch ? "/api/companion-matches/" + ids.matchId()
                : "/api/companion-meetings/" + ids.meetingId();
        return mvc.perform(post(path + "/reports").with(jwt().jwt(token -> token.subject(Long.toString(userId))))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String body(final long target) {
        return "{\"reportedUserId\":" + target + ",\"reasons\":[\"NO_SHOW\"]}";
    }

    private Ids fixture(final Stage stage) throws Exception {
        long host = user();
        long guest = user();
        long place = insert("""
                insert into place_cache (google_place_id, name, address, latitude, longitude, business_status)
                values (?, '장소', 'Paris, France', 48.85, 2.35, 'OPERATIONAL')
                """, "report242-" + host);
        long post = insert("""
                insert into companion_post (host_user_id, place_id, meeting_at, max_participants, content,
                    open_chat_url, status, created_at, meeting_time_type)
                values (?, ?, null, 2, '동행 본문', 'https://example.com', 'CLOSED', ?, 'UNDECIDED')
                """, host, place, NOW);
        long match = insert("insert into companion_match (post_id, status, created_at) values (?, 'MATCHED', ?)", post, NOW);
        sql("insert into companion_match_participant (match_id, user_id, role) values (?, ?, 'HOST')", match, host);
        sql("insert into companion_match_participant (match_id, user_id, role) values (?, ?, 'GUEST')", match, guest);
        Ids pending = new Ids(host, guest, place, match, null);
        return stage == Stage.SCHEDULING ? pending : confirm(pending, stage);
    }

    private Ids confirm(final Ids pending, final Stage stage) throws Exception {
        boolean completed = stage == Stage.COMPLETED;
        sql("update companion_match set status = ? where id = ?",
                completed ? "COMPLETED" : "SCHEDULE_CONFIRMED", pending.matchId());
        sql("insert into companion_schedule (match_id, place_id, scheduled_at, confirmed) values (?, ?, ?, true)",
                pending.matchId(), pending.placeId(), NOW);
        long meeting = insert("insert into companion_meeting (match_id, status, started_at, completed_at) values (?, ?, ?, ?)",
                pending.matchId(), completed ? "COMPLETED" : "ONGOING", NOW, completed ? NOW : null);
        if (stage != Stage.BEFORE_CHECK_IN) {
            boolean individuallyCompleted = stage == Stage.REVIEWING || stage == Stage.REVIEWED || completed;
            checkIn(meeting, pending.hostId(), individuallyCompleted);
            if (stage != Stage.NO_SHOW) {
                checkIn(meeting, pending.guestId(), completed);
            }
        }
        if (stage == Stage.REVIEWED || completed) {
            sql("""
                    insert into companion_review (meeting_id, reviewer_user_id, reviewee_user_id, rating, created_at)
                    values (?, ?, ?, 1, ?)
                    """, meeting, pending.hostId(), pending.guestId(), NOW);
        }
        return new Ids(pending.hostId(), pending.guestId(), pending.placeId(), pending.matchId(), meeting);
    }

    private void checkIn(final long meetingId, final long userId, final boolean completed) throws SQLException {
        sql("""
                insert into meeting_check_in (meeting_id, user_id, latitude, longitude, checked_in_at, completed_at)
                values (?, ?, 48.85, 2.35, ?, ?)
                """, meetingId, userId, NOW, completed ? NOW : null);
    }

    private long user() {
        long id = users.save(new UserAccount(null, UserRole.USER, UserAccountStatus.ACTIVE, "01012345678",
                NOW, UserOnboardingStatus.COMPLETED, NOW, null)).id();
        profiles.save(new CompanionProfile(null, id, "사용자" + id, UserGender.FEMALE, 1998, null, null,
                BigDecimal.ZERO, 0, CompanionProfileStatus.ACTIVE));
        return id;
    }

    private long insert(final String sql, final Object... values) throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                return keys.getLong(1);
            }
        }
    }

    private void sql(final String sql, final Object... values) throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            statement.executeUpdate();
        }
    }

    private long count(final String sql, final Object... values) throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getLong(1);
            }
        }
    }

    private enum Stage { SCHEDULING, BEFORE_CHECK_IN, NO_SHOW, ONGOING, REVIEWING, REVIEWED, COMPLETED }
    private record Ids(long hostId, long guestId, long placeId, long matchId, Long meetingId) {}
}
