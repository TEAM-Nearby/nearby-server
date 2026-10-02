// 본인 프로필 수정의 HTTP 계약과 원자성·동시성을 실제 DB 경계에서 검증한다.
package com.sopt.nearby.companion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sopt.nearby.companion.application.UpdateMyCompanionProfileCommand;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStyle;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.in.UpdateMyCompanionProfileUseCase;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileStyleRepository;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(properties = {
        "spring.datasource.url=${ISSUE213_TEST_DB_URL:jdbc:h2:mem:profile213;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000}",
        "spring.datasource.username=${ISSUE213_TEST_DB_USERNAME:sa}",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "management.health.redis.enabled=false"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class MyCompanionProfileFlowTest {
    private static final String PATH = "/api/users/me/companion-profile";
    @Autowired private MockMvc mvc;
    @Autowired private UserAccountRepository users;
    @Autowired private CompanionProfileRepository profiles;
    @Autowired private CompanionProfileStyleRepository styles;
    @Autowired private UpdateMyCompanionProfileUseCase updateProfile;
    @Autowired private DataSource dataSource;

    @Test
    void readsInitialValuesAndUpdatesOnlyOwnEditableFieldsAcrossReadApis() throws Exception {
        CompanionProfile own = profile(CompanionProfileStatus.ACTIVE);
        CompanionProfile other = profile(CompanionProfileStatus.ACTIVE);
        UserAccount accountBefore = users.findById(own.userId()).orElseThrow();
        mvc.perform(get(PATH).with(jwt().jwt(token -> token.subject(own.userId().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileId").value(own.id()))
                .andExpect(jsonPath("$.data.intro").value("처음 소개"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.birthYear").value(1998));

        String nickname = "수정" + own.id();
        String body = """
                {"nickname":"%s","intro":"새로운 소개","profileImageUrl":"https://example.com/new.png",
                 "travelStyleKeywords":["FOODIE","CAFE_TOUR"],
                 "userId":%s,"profileId":%s,"gender":"MALE","birthYear":2000,"mannerScore":0,"reviewCount":0}
                """.formatted(nickname, other.userId(), other.id());
        for (int retry = 0; retry < 2; retry++) {
            putProfile(own.userId(), body).andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("COMPANION_PROFILE_UPDATED"))
                    .andExpect(jsonPath("$.data.nickname").value(nickname))
                    .andExpect(jsonPath("$.data.travelStyleKeywords", containsInAnyOrder("FOODIE", "CAFE_TOUR")));
        }
        CompanionProfile saved = profiles.findById(own.id()).orElseThrow();
        assertThat(saved.userId()).isEqualTo(own.userId());
        assertThat(saved.gender()).isEqualTo(own.gender());
        assertThat(saved.birthYear()).isEqualTo(own.birthYear());
        assertThat(saved.mannerScore()).isEqualByComparingTo(own.mannerScore());
        assertThat(saved.reviewCount()).isEqualTo(own.reviewCount());
        assertThat(saved.status()).isEqualTo(own.status());
        assertThat(users.findById(own.userId()).orElseThrow()).isEqualTo(accountBefore);
        assertThat(profiles.findById(other.id()).orElseThrow()).isEqualTo(other);
        assertThat(keywords(own.id())).containsExactlyInAnyOrder(TravelStyleKeyword.FOODIE, TravelStyleKeyword.CAFE_TOUR);
        assertThat(keywords(other.id())).containsExactlyInAnyOrder(TravelStyleKeyword.EXTROVERTED, TravelStyleKeyword.FOODIE);

        mvc.perform(get(PATH).with(jwt().jwt(token -> token.subject(own.userId().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.intro").value("새로운 소개"))
                .andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://example.com/new.png"));
        mvc.perform(get("/api/users/me/mypage").with(jwt().jwt(token -> token.subject(own.userId().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.nickname").value(nickname))
                .andExpect(jsonPath("$.data.travelStyleKeywords", containsInAnyOrder("FOODIE", "CAFE_TOUR")));
        mvc.perform(get("/api/companion-profiles/" + own.id())
                        .with(jwt().jwt(token -> token.subject(own.userId().toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.intro").value("새로운 소개"))
                .andExpect(jsonPath("$.data.keywords", containsInAnyOrder("FOODIE", "CAFE_TOUR")));
    }

    @Test
    void keepsOwnNicknameAndRejectsAnotherUsersNicknameWithoutChangingAnything() throws Exception {
        CompanionProfile own = profile(CompanionProfileStatus.ACTIVE);
        CompanionProfile other = profile(CompanionProfileStatus.ACTIVE);
        putProfile(own.userId(), body(own.nickname(), "INTROVERTED")).andExpect(status().isOk());
        CompanionProfile before = profiles.findById(own.id()).orElseThrow();
        putProfile(own.userId(), body(other.nickname(), "CAFE_TOUR"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPLICATE_NICKNAME"));
        assertThat(profiles.findById(own.id()).orElseThrow()).isEqualTo(before);
        assertThat(keywords(own.id())).containsExactly(TravelStyleKeyword.INTROVERTED);
    }

    @Test
    void clearsOptionalFieldsForNullEmptyWhitespaceAndMissingFields() throws Exception {
        CompanionProfile own = profile(CompanionProfileStatus.ACTIVE);
        for (String optionalFields : List.of(
                ",\"intro\":null,\"profileImageUrl\":null",
                ",\"intro\":\"\",\"profileImageUrl\":\"\"",
                ",\"intro\":\"   \",\"profileImageUrl\":\"   \"", "")) {
            putProfile(own.userId(), """
                    {"nickname":"%s","intro":"소개","profileImageUrl":"https://example.com/a.png",
                     "travelStyleKeywords":["FOODIE"]}
                    """.formatted(own.nickname())).andExpect(status().isOk());
            putProfile(own.userId(), "{\"nickname\":\"" + own.nickname()
                    + "\",\"travelStyleKeywords\":[\"FOODIE\"]" + optionalFields + "}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.intro").value(nullValue()))
                    .andExpect(jsonPath("$.data.profileImageUrl").value(nullValue()));
            CompanionProfile saved = profiles.findById(own.id()).orElseThrow();
            assertThat(saved.intro()).isNull();
            assertThat(saved.profileImageUrl()).isNull();
        }
    }

    @Test
    void rejectsInvalidAndIncompleteBodiesWithoutSaving() throws Exception {
        CompanionProfile own = profile(CompanionProfileStatus.ACTIVE);
        for (String body : List.of("{}", "{\"nickname\":\"친구\"}",
                "{\"travelStyleKeywords\":[\"FOODIE\"]}", body(" ", "FOODIE"), body("가".repeat(16), "FOODIE"),
                body("친구", "UNKNOWN"), body("친구", "FOODIE\",\"FOODIE"),
                "{\"nickname\":\"친구\",\"travelStyleKeywords\":[]}",
                "{\"nickname\":\"친구\",\"travelStyleKeywords\":[null]}",
                "{\"nickname\":\"친구\",\"travelStyleKeywords\":[\"FOODIE\"],\"profileImageUrl\":\"invalid\"}",
                "{\"nickname\":\"친구\",\"travelStyleKeywords\":[\"FOODIE\"],\"intro\":\"" + "가".repeat(51) + "\"}",
                "{broken")) {
            putProfile(own.userId(), body).andExpect(status().isBadRequest());
        }
        assertThat(profiles.findById(own.id()).orElseThrow()).isEqualTo(own);
        assertThat(keywords(own.id())).containsExactlyInAnyOrder(TravelStyleKeyword.EXTROVERTED, TravelStyleKeyword.FOODIE);
    }

    @Test
    void requiresAuthenticationCompletedOnboardingAndAnActiveExistingProfile() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(body("친구", "FOODIE")))
                .andExpect(status().isUnauthorized());
        assertAccessDenied(user(UserOnboardingStatus.STARTED).id(), "ONBOARDING_REQUIRED");
        assertAccessDenied(user(UserOnboardingStatus.COMPANION_PROFILE_SKIPPED).id(), "COMPANION_PROFILE_REQUIRED");
        for (CompanionProfileStatus status : List.of(CompanionProfileStatus.INACTIVE, CompanionProfileStatus.SKIPPED)) {
            CompanionProfile inactive = profile(status);
            assertAccessDenied(inactive.userId(), "FORBIDDEN_INACTIVE_COMPANION_PROFILE");
            assertThat(profiles.findById(inactive.id()).orElseThrow()).isEqualTo(inactive);
        }
        putProfile(Long.MAX_VALUE, body("친구", "FOODIE"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void rollsBackProfileAndStyleReplacementWhenTheDatabaseRejectsANewStyle() throws Exception {
        CompanionProfile own = profile(CompanionProfileStatus.ACTIVE);
        String constraint = "issue213_style_failure_" + own.id();
        sql("alter table companion_profile_style add constraint " + constraint
                + " check (profile_id <> " + own.id() + " or keyword <> 'INTROVERTED')");
        try {
            assertThatThrownBy(() -> updateProfile.update(new UpdateMyCompanionProfileCommand(
                    own.userId(), "실패" + own.id(), "저장 실패", null,
                    List.of(TravelStyleKeyword.CAFE_TOUR, TravelStyleKeyword.INTROVERTED))))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(profiles.findById(own.id()).orElseThrow()).isEqualTo(own);
            assertThat(keywords(own.id())).containsExactlyInAnyOrder(TravelStyleKeyword.EXTROVERTED, TravelStyleKeyword.FOODIE);
        } finally {
            sql("alter table companion_profile_style drop constraint " + constraint);
        }
    }

    @Test
    void concurrentUpdatesOfTheSameProfileKeepOneCompleteRequest() throws Exception {
        CompanionProfile own = profile(CompanionProfileStatus.ACTIVE);
        String first = "동시가" + own.id();
        String second = "동시나" + own.id();
        List<Integer> statuses = concurrently(List.of(
                () -> putProfile(own.userId(), body(first, "CAFE_TOUR")).andReturn().getResponse().getStatus(),
                () -> putProfile(own.userId(), body(second, "INTROVERTED")).andReturn().getResponse().getStatus()));
        assertThat(statuses).containsOnly(200);
        CompanionProfile saved = profiles.findById(own.id()).orElseThrow();
        assertThat(saved.nickname()).isIn(first, second);
        assertThat(keywords(own.id())).containsExactly(saved.nickname().equals(first)
                ? TravelStyleKeyword.CAFE_TOUR : TravelStyleKeyword.INTROVERTED);
    }

    @Test
    void concurrentUsersCompetingForOneNicknameReceiveOneSuccessAndOneConflict() throws Exception {
        CompanionProfile first = profile(CompanionProfileStatus.ACTIVE);
        CompanionProfile second = profile(CompanionProfileStatus.ACTIVE);
        String nickname = "경쟁" + first.id();
        List<Integer> statuses = concurrently(List.of(
                () -> putProfile(first.userId(), body(nickname, "CAFE_TOUR")).andReturn().getResponse().getStatus(),
                () -> putProfile(second.userId(), body(nickname, "CAFE_TOUR")).andReturn().getResponse().getStatus()));
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        CompanionProfile loser = profiles.findById(first.id()).orElseThrow().nickname().equals(nickname) ? second : first;
        assertThat(profiles.findById(loser.id()).orElseThrow()).isEqualTo(loser);
        assertThat(keywords(loser.id())).containsExactlyInAnyOrder(TravelStyleKeyword.EXTROVERTED, TravelStyleKeyword.FOODIE);
    }

    @Test
    void documentsReadUpdateAndBusinessErrors() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['" + PATH + "'].get").exists())
                .andExpect(jsonPath("$.paths['" + PATH + "'].put.responses['409'].content['application/json']"
                        + ".examples.DUPLICATE_NICKNAME.value.code").value("DUPLICATE_NICKNAME"))
                .andExpect(jsonPath("$.paths['" + PATH + "'].put.responses['403'].content['application/json']"
                        + ".examples.FORBIDDEN_INACTIVE_COMPANION_PROFILE.value.code").value("FORBIDDEN_INACTIVE_COMPANION_PROFILE"));
    }

    private ResultActions putProfile(final Long userId, final String body) throws Exception {
        return mvc.perform(put(PATH).with(jwt().jwt(token -> token.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String body(final String nickname, final String keyword) {
        return "{\"nickname\":\"" + nickname + "\",\"travelStyleKeywords\":[\"" + keyword + "\"]}";
    }

    private void assertAccessDenied(final Long userId, final String code) throws Exception {
        mvc.perform(get(PATH).with(jwt().jwt(token -> token.subject(userId.toString()))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(code));
        putProfile(userId, body("친구", "FOODIE"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(code));
    }

    private UserAccount user(final UserOnboardingStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 10, 2, 0, 0);
        return users.save(new UserAccount(null, UserRole.USER, UserAccountStatus.ACTIVE,
                "01012345678", now, status, now, null));
    }

    private CompanionProfile profile(final CompanionProfileStatus status) {
        Long userId = user(UserOnboardingStatus.COMPLETED).id();
        CompanionProfile profile = profiles.save(new CompanionProfile(null, userId, "사용자" + userId,
                UserGender.FEMALE, 1998, "https://example.com/old.png", "처음 소개", new BigDecimal("4.50"), 3, status));
        styles.save(new CompanionProfileStyle(profile.id(), TravelStyleKeyword.EXTROVERTED));
        styles.save(new CompanionProfileStyle(profile.id(), TravelStyleKeyword.FOODIE));
        return profile;
    }

    private List<TravelStyleKeyword> keywords(final Long profileId) {
        return styles.findAllByProfileId(profileId).stream().map(CompanionProfileStyle::keyword).toList();
    }

    private void sql(final String statement) throws Exception {
        try (var connection = dataSource.getConnection(); var query = connection.createStatement()) {
            query.execute(statement);
        }
    }

    private <T> List<T> concurrently(final List<Callable<T>> requests) throws Exception {
        try (var executor = Executors.newFixedThreadPool(requests.size())) {
            CountDownLatch ready = new CountDownLatch(requests.size());
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> results = new ArrayList<>();
            for (Callable<T> request : requests) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("동시 요청 시작 대기 시간 초과");
                    }
                    return request.call();
                }));
            }
            try {
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            List<T> responses = new ArrayList<>();
            for (Future<T> result : results) {
                responses.add(result.get(15, TimeUnit.SECONDS));
            }
            return responses;
        }
    }
}
