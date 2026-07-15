// 동행 매칭 미리보기 컨트롤러의 인증 사용자 전달과 응답 형식을 검증하는 테스트
package com.sopt.nearby.companion.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sopt.nearby.companion.application.ConfirmCompanionScheduleCommand;
import com.sopt.nearby.companion.application.ConfirmCompanionScheduleResult;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionMatchException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
import com.sopt.nearby.companion.domain.model.match.CompanionScheduleDetail;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.in.ConfirmCompanionScheduleUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchPreviewUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchesUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionScheduleUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompanionMatchControllerTest {

    private MockMvc mockMvc;
    private FakeReadCompanionMatchPreviewUseCase useCase;
    private FakeReadCompanionMatchesUseCase readCompanionMatchesUseCase;
    private FakeConfirmCompanionScheduleUseCase confirmCompanionScheduleUseCase;
    private FakeReadCompanionScheduleUseCase readCompanionScheduleUseCase;

    @BeforeEach
    void setUp() {
        useCase = new FakeReadCompanionMatchPreviewUseCase();
        readCompanionMatchesUseCase = new FakeReadCompanionMatchesUseCase();
        confirmCompanionScheduleUseCase = new FakeConfirmCompanionScheduleUseCase();
        readCompanionScheduleUseCase = new FakeReadCompanionScheduleUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanionMatchController(
                        useCase,
                        readCompanionMatchesUseCase,
                        confirmCompanionScheduleUseCase,
                        readCompanionScheduleUseCase
                ))
                .setMessageConverters(jsonMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void passesAuthenticatedUserIdFromPrincipalToUseCase() throws Exception {
        useCase.result = new CompanionMatchPreview(
                10L,
                new CompanionMatchPreview.Member(7L, "https://image.example/a.png", "여행자A"),
                List.of(
                        new CompanionMatchPreview.Member(8L, null, "여행자B")
                ),
                new CompanionMatchPreview.Post(
                        20L,
                        "함께 밥 먹을 동행을 구해요.",
                        "바르셀로나 고딕 지구",
                        CompanionPostMeetingTimeType.SCHEDULED,
                        LocalDateTime.of(2026, 6, 29, 18, 30)
                )
        );

        mockMvc.perform(get("/api/companion-matches/{matchId}/preview", 10L)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_COMPANION_MATCH_PREVIEW"))
                .andExpect(jsonPath("$.message").value("매칭된 동행 미리보기 정보를 조회했어요."))
                .andExpect(jsonPath("$.data.matchId").value(10))
                .andExpect(jsonPath("$.data.host.hostName").value("여행자A"))
                .andExpect(jsonPath("$.data.host.hostProfileImageUrl").value("https://image.example/a.png"))
                .andExpect(jsonPath("$.data.members.length()").value(1))
                .andExpect(jsonPath("$.data.members[0].memberId").value(8))
                .andExpect(jsonPath("$.data.members[0].profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.members[0].nickname").value("여행자B"))
                .andExpect(jsonPath("$.data.companionPost.postId").value(20))
                .andExpect(jsonPath("$.data.companionPost.content").value("함께 밥 먹을 동행을 구해요."))
                .andExpect(jsonPath("$.data.companionPost.placeName").value("바르셀로나 고딕 지구"))
                .andExpect(jsonPath("$.data.companionPost.meetingTimeType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.companionPost.meetingAt").value("2026-06-29T18:30:00"));

        assertEquals(10L, useCase.matchId);
        assertEquals(7L, useCase.userId);
    }

    @Test
    void passesAuthenticatedUserIdFromPrincipalToReadMatchesUseCase() throws Exception {
        readCompanionMatchesUseCase.result = List.of(new CompanionMatchSummary(
                1L,
                "호스트A",
                "https://image.example/host-a.png",
                UserGender.FEMALE,
                "시우다드콘달",
                LocalDateTime.of(2026, 6, 29, 18, 30),
                CompanionPostMeetingTimeType.SCHEDULED,
                LocalDateTime.of(2026, 6, 29, 18, 15),
                "오늘 저녁 바르셀로나에서 같이 타파스 드실 분 구해요",
                CompanionMatchStatus.MATCHED
        ));

        mockMvc.perform(get("/api/companion-matches")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_COMPANION_MATCHES"))
                .andExpect(jsonPath("$.message").value("매칭된 동행 목록을 조회했어요."))
                .andExpect(jsonPath("$.data.matches[0].matchId").value(1))
                .andExpect(jsonPath("$.data.matches[0].hostNickname").value("호스트A"))
                .andExpect(jsonPath("$.data.matches[0].hostProfileImageUrl")
                        .value("https://image.example/host-a.png"))
                .andExpect(jsonPath("$.data.matches[0].hostGender").value("FEMALE"))
                .andExpect(jsonPath("$.data.matches[0].placeName").value("시우다드콘달"))
                .andExpect(jsonPath("$.data.matches[0].meetingAt").value("2026-06-29T18:30:00"))
                .andExpect(jsonPath("$.data.matches[0].meetingTimeType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.matches[0].createdAt").value("2026-06-29T18:15:00"))
                .andExpect(jsonPath("$.data.matches[0].content")
                        .value("오늘 저녁 바르셀로나에서 같이 타파스 드실 분 구해요"))
                .andExpect(jsonPath("$.data.matches[0].matchStatus").value("MATCHED"));

        assertEquals(7L, readCompanionMatchesUseCase.userId);
    }

    @Test
    void returnsForbiddenWhenRequesterIsNotMatchParticipant() throws Exception {
        useCase.exception = new ForbiddenCompanionMatchException();

        mockMvc.perform(get("/api/companion-matches/{matchId}/preview", 10L)
                        .principal(principal("7")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN_COMPANION_MATCH"))
                .andExpect(jsonPath("$.message").value("해당 매칭 정보를 조회할 권한이 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void passesScheduledAtAndAuthenticatedUserIdToScheduleUseCase() throws Exception {
        confirmCompanionScheduleUseCase.result = new ConfirmCompanionScheduleResult(
                10L,
                99L,
                CompanionMatchStatus.SCHEDULE_CONFIRMED
        );

        mockMvc.perform(patch("/api/companion-matches/{matchId}/schedule", 10L)
                        .principal(principal("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scheduledAt": "2026-07-04T16:22:29"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("CONFIRM_COMPANION_SCHEDULE"))
                .andExpect(jsonPath("$.message").value("동행 일정이 수정되었어요."))
                .andExpect(jsonPath("$.data.matchId").value(10))
                .andExpect(jsonPath("$.data.scheduleId").value(99))
                .andExpect(jsonPath("$.data.matchStatus").value("SCHEDULE_CONFIRMED"));

        ConfirmCompanionScheduleCommand command = confirmCompanionScheduleUseCase.command;
        assertEquals(10L, command.matchId());
        assertEquals(7L, command.requesterUserId());
        assertEquals(LocalDateTime.of(2026, 7, 4, 16, 22, 29), command.scheduledAt());
    }

    @Test
    void returnsConfirmedScheduleDetailAndPassesAuthenticatedUserIdToUseCase() throws Exception {
        readCompanionScheduleUseCase.result = new CompanionScheduleDetail(
                10L,
                CompanionMatchStatus.SCHEDULE_CONFIRMED,
                new CompanionScheduleDetail.Schedule(
                        new CompanionScheduleDetail.Place(
                                "google-place-id",
                                "Siutat condal",
                                "Rambla de Catalunya, 16",
                                new BigDecimal("41.390205"),
                                new BigDecimal("2.163548")
                        ),
                        LocalDateTime.of(2026, 6, 18, 16, 30)
                ),
                "https://open.kakao.com/o/confirmed",
                "루피",
                CompanionPostMeetingTimeType.SCHEDULED,
                MatchParticipantRole.HOST
        );

        mockMvc.perform(get("/api/companion-matches/{matchId}/schedule", 10L)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_COMPANION_SCHEDULE"))
                .andExpect(jsonPath("$.message").value("동행 일정 정보를 조회했어요."))
                .andExpect(jsonPath("$.data.matchId").value(10))
                .andExpect(jsonPath("$.data.matchStatus").value("SCHEDULE_CONFIRMED"))
                .andExpect(jsonPath("$.data.schedule.place.googlePlaceId").value("google-place-id"))
                .andExpect(jsonPath("$.data.schedule.place.name").value("Siutat condal"))
                .andExpect(jsonPath("$.data.schedule.place.address").value("Rambla de Catalunya, 16"))
                .andExpect(jsonPath("$.data.schedule.place.latitude").value(41.390205))
                .andExpect(jsonPath("$.data.schedule.place.longitude").value(2.163548))
                .andExpect(jsonPath("$.data.schedule.scheduledAt").value("2026-06-18T16:30:00"))
                .andExpect(jsonPath("$.data.openChatUrl").value("https://open.kakao.com/o/confirmed"))
                .andExpect(jsonPath("$.data.userNickname").value("루피"))
                .andExpect(jsonPath("$.data.meetingTimeType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.currentUserRole").value("HOST"));

        assertEquals(10L, readCompanionScheduleUseCase.matchId);
        assertEquals(7L, readCompanionScheduleUseCase.userId);
    }

    @Test
    void returnsNowScheduleAndOpenChatUrlWhenCompanionScheduleIsNotConfirmed() throws Exception {
        readCompanionScheduleUseCase.result = new CompanionScheduleDetail(
                10L,
                CompanionMatchStatus.SCHEDULE_CONFIRMED,
                new CompanionScheduleDetail.Schedule(
                        new CompanionScheduleDetail.Place(
                                "google-place-id",
                                "Siutat condal",
                                "Rambla de Catalunya, 16",
                                new BigDecimal("41.390205"),
                                new BigDecimal("2.163548")
                        ),
                        LocalDateTime.of(2026, 6, 18, 13, 0)
                ),
                "https://open.kakao.com/o/not-yet",
                "루피",
                CompanionPostMeetingTimeType.NOW,
                MatchParticipantRole.GUEST
        );

        mockMvc.perform(get("/api/companion-matches/{matchId}/schedule", 10L)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_COMPANION_SCHEDULE"))
                .andExpect(jsonPath("$.message").value("동행 일정 정보를 조회했어요."))
                .andExpect(jsonPath("$.data.matchId").value(10))
                .andExpect(jsonPath("$.data.matchStatus").value("SCHEDULE_CONFIRMED"))
                .andExpect(jsonPath("$.data.schedule.place.googlePlaceId").value("google-place-id"))
                .andExpect(jsonPath("$.data.schedule.place.name").value("Siutat condal"))
                .andExpect(jsonPath("$.data.schedule.place.address").value("Rambla de Catalunya, 16"))
                .andExpect(jsonPath("$.data.schedule.place.latitude").value(41.390205))
                .andExpect(jsonPath("$.data.schedule.place.longitude").value(2.163548))
                .andExpect(jsonPath("$.data.schedule.scheduledAt").value("2026-06-18T13:00:00"))
                .andExpect(jsonPath("$.data.openChatUrl").value("https://open.kakao.com/o/not-yet"))
                .andExpect(jsonPath("$.data.userNickname").value("루피"))
                .andExpect(jsonPath("$.data.meetingTimeType").value("NOW"))
                .andExpect(jsonPath("$.data.currentUserRole").value("GUEST"));
    }

    private Principal principal(final String name) {
        return () -> name;
    }

    private MappingJackson2HttpMessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    private static final class FakeReadCompanionMatchPreviewUseCase implements ReadCompanionMatchPreviewUseCase {

        private CompanionMatchPreview result;
        private RuntimeException exception;
        private Long matchId;
        private Long userId;

        @Override
        public CompanionMatchPreview getPreview(final Long matchId, final Long userId) {
            this.matchId = matchId;
            this.userId = userId;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }

    private static final class FakeReadCompanionMatchesUseCase implements ReadCompanionMatchesUseCase {

        private List<CompanionMatchSummary> result = List.of();
        private Long userId;

        @Override
        public List<CompanionMatchSummary> getMatches(final Long userId) {
            this.userId = userId;
            return result;
        }
    }

    private static final class FakeConfirmCompanionScheduleUseCase implements ConfirmCompanionScheduleUseCase {

        private ConfirmCompanionScheduleResult result;
        private ConfirmCompanionScheduleCommand command;

        @Override
        public ConfirmCompanionScheduleResult update(final ConfirmCompanionScheduleCommand command) {
            this.command = command;
            return result;
        }
    }

    private static final class FakeReadCompanionScheduleUseCase implements ReadCompanionScheduleUseCase {

        private CompanionScheduleDetail result;
        private Long matchId;
        private Long userId;

        @Override
        public CompanionScheduleDetail getSchedule(final Long matchId, final Long userId) {
            this.matchId = matchId;
            this.userId = userId;
            return result;
        }
    }
}
