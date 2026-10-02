// 온보딩 선택 등록과 동행 접근 제한을 실제 HTTP·DB 경계에서 검증한다.
package com.sopt.nearby.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sopt.nearby.companion.application.RegisterCompanionProfileCommand;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.in.RegisterCompanionProfileUseCase;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.user.application.SaveEmergencyContactCommand;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.port.in.EmergencyContactUseCase;
import com.sopt.nearby.user.port.in.SkipCompanionProfileUseCase;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:onboarding210;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=validate",
        "management.health.redis.enabled=false"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class OnboardingFlowTest {
    @Autowired private MockMvc mvc;
    @Autowired private UserAccountRepository users;
    @Autowired private EmergencyContactUseCase contacts;
    @Autowired private SkipCompanionProfileUseCase skipProfile;
    @Autowired private RegisterCompanionProfileUseCase registerProfile;
    @Autowired private CompanionProfileRepository profiles;
    @Autowired private DataSource dataSource;

    @Test
    void skipsWithoutEmergencyContactAndCanRegisterLaterUsingTheSameToken() throws Exception {
        Long id = user(UserOnboardingStatus.PHONE_VERIFIED).id();
        var authentication = jwt().jwt(token -> token.subject(id.toString()).claim("onboardingStatus", "PHONE_VERIFIED"));

        mvc.perform(post("/api/onboarding/companion-profiles/skip").with(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboardingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.hasCompanionProfile").value(false))
                .andExpect(jsonPath("$.data.hasEmergencyContact").value(false));
        mvc.perform(post("/api/onboarding/companion-profiles/skip").with(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasCompanionProfile").value(false));
        assertThat(profiles.findByUserId(id)).isEmpty();
        assertThat(skipProfile.skip(id).onboardingStatus()).isEqualTo(UserOnboardingStatus.COMPANION_PROFILE_SKIPPED);

        mvc.perform(get("/api/onboarding").with(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phoneVerified").value(true))
                .andExpect(jsonPath("$.data.hasCompanionProfile").value(false));
        mvc.perform(get("/api/users/me/mypage").with(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasCompanionProfile").value(false))
                .andExpect(jsonPath("$.data.nickname").value(nullValue()))
                .andExpect(jsonPath("$.data.gender").value(nullValue()))
                .andExpect(jsonPath("$.data.travelStyleKeywords").isEmpty())
                .andExpect(jsonPath("$.data.mealTogetherCount").value(0));
        mvc.perform(get("/api/companion-matches").with(authentication))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANION_PROFILE_REQUIRED"));

        mvc.perform(post("/api/onboarding/companion-profiles").with(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"사용자%s","gender":"FEMALE","travelStyleKeywords":["EXTROVERTED"]}
                                """.formatted(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onboardingStatus").value("COMPLETED"));
        mvc.perform(get("/api/onboarding").with(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasCompanionProfile").value(true));
        mvc.perform(get("/api/companion-matches").with(authentication)).andExpect(status().isOk());
        mvc.perform(get("/api/users/me/mypage").with(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasCompanionProfile").value(true));
        mvc.perform(post("/api/onboarding/companion-profiles/skip").with(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasCompanionProfile").value(true));
        assertThat(users.findById(id).orElseThrow().onboardingStatus()).isEqualTo(UserOnboardingStatus.COMPLETED);
    }

    @Test
    void blocksAllCompanionPageFamiliesForSkippedUsers() throws Exception {
        Long id = user(UserOnboardingStatus.COMPANION_PROFILE_SKIPPED).id();
        for (String path : List.of("/api/companion-posts", "/api/companion-posts/1",
                "/api/companion-profiles/1", "/api/companion-requests/1/review",
                "/api/companion-matches", "/api/companion-matches/1/schedule", "/api/companion-meetings/1",
                "/api/users/me/recruitment-posts", "/api/users/me/companion-requests/page",
                "/api/users/me/companion-requests/1/result")) {
            mvc.perform(get(path).with(jwt().jwt(token -> token.subject(id.toString()))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("COMPANION_PROFILE_REQUIRED"));
        }
        mvc.perform(post("/api/companion-posts").with(jwt().jwt(token -> token.subject(id.toString()))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/companion-posts/1/companion-requests")
                        .with(jwt().jwt(token -> token.subject(id.toString()))))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/companion-requests/1/accept")
                        .with(jwt().jwt(token -> token.subject(id.toString()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsSkippingBeforePhoneVerificationAndUnauthenticatedRequests() throws Exception {
        Long id = user(UserOnboardingStatus.STARTED).id();
        mvc.perform(post("/api/onboarding/companion-profiles/skip")
                        .with(jwt().jwt(token -> token.subject(id.toString()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PHONE_VERIFICATION_REQUIRED"));
        mvc.perform(get("/api/companion-matches").with(jwt().jwt(token -> token.subject(id.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
        mvc.perform(post("/api/onboarding/companion-profiles/skip")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/onboarding/emergency-contact")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/onboarding")).andExpect(status().isUnauthorized());
        assertThat(users.findById(id).orElseThrow().onboardingStatus()).isEqualTo(UserOnboardingStatus.STARTED);
    }

    @Test
    void contactIsOptionalOwnedByAuthenticatedUserAndRepeatedSaveKeepsOneRow() throws Exception {
        Long id = user(UserOnboardingStatus.PHONE_VERIFIED).id();
        Long otherId = user(UserOnboardingStatus.PHONE_VERIFIED).id();
        var authentication = jwt().jwt(token -> token.subject(id.toString()));
        mvc.perform(get("/api/onboarding/emergency-contact").with(authentication))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(nullValue()));
        for (int retry = 0; retry < 2; retry++) {
            mvc.perform(put("/api/onboarding/emergency-contact").with(authentication)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"가족","phoneNumber":"01012345678","userId":%s}
                                    """.formatted(otherId)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("가족"));
        }
        Long contactId = contacts.read(id).orElseThrow().id();
        mvc.perform(put("/api/onboarding/emergency-contact").with(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"친구","phoneNumber":"+821098765432"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.contactId").value(contactId));
        assertThat(countContacts(id)).isEqualTo(1);
        assertThat(contacts.read(otherId)).isEmpty();
        assertThat(users.findById(id).orElseThrow().onboardingStatus()).isEqualTo(UserOnboardingStatus.PHONE_VERIFIED);
        mvc.perform(get("/api/onboarding").with(authentication))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.hasEmergencyContact").value(true));
    }

    @Test
    void rejectsInvalidContactWithoutSavingIt() throws Exception {
        Long id = user(UserOnboardingStatus.PHONE_VERIFIED).id();
        for (String body : List.of(
                "{\"name\":\" \",\"phoneNumber\":\"01012345678\"}",
                "{\"name\":\"가족\",\"phoneNumber\":\"not-a-phone\"}",
                "{\"name\":\"가족\",\"phoneNumber\":\"123\"}",
                "{\"name\":\"가족\"}")) {
            mvc.perform(put("/api/onboarding/emergency-contact").with(jwt().jwt(token -> token.subject(id.toString())))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertThat(countContacts(id)).isZero();
    }

    @Test
    void rollsBackOnboardingCompletionWhenProfilePersistenceFails() {
        Long id = user(UserOnboardingStatus.COMPANION_PROFILE_SKIPPED).id();
        assertThatThrownBy(() -> registerProfile.register(new RegisterCompanionProfileCommand(
                id, "실패" + id, null, null, null, List.of(TravelStyleKeyword.EXTROVERTED))))
                .isInstanceOf(RuntimeException.class);
        assertThat(users.findById(id).orElseThrow().onboardingStatus())
                .isEqualTo(UserOnboardingStatus.COMPANION_PROFILE_SKIPPED);
        assertThat(profiles.findByUserId(id)).isEmpty();
    }

    @Test
    void serializesConcurrentContactRegistration() throws Exception {
        Long id = user(UserOnboardingStatus.PHONE_VERIFIED).id();
        List<Runnable> requests = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            requests.add(() -> contacts.save(new SaveEmergencyContactCommand(id, "가족", "01012345678")));
        }
        concurrently(requests);
        assertThat(countContacts(id)).isEqualTo(1);
    }

    @Test
    void concurrentSkipAndRegistrationAlwaysEndsWithRegisteredProfile() throws Exception {
        Long id = user(UserOnboardingStatus.PHONE_VERIFIED).id();
        concurrently(List.of(
                () -> skipProfile.skip(id),
                () -> registerProfile.register(new RegisterCompanionProfileCommand(
                        id, "동시" + id, UserGender.FEMALE, null, null, List.of(TravelStyleKeyword.EXTROVERTED)))
        ));
        assertThat(users.findById(id).orElseThrow().onboardingStatus()).isEqualTo(UserOnboardingStatus.COMPLETED);
        assertThat(profiles.findByUserId(id)).isPresent();
    }

    @Test
    void databaseRejectsSecondContactForTheSameUser() throws Exception {
        Long id = user(UserOnboardingStatus.PHONE_VERIFIED).id();
        contacts.save(new SaveEmergencyContactCommand(id, "가족", "01012345678"));
        try (var connection = dataSource.getConnection();
             var insert = connection.prepareStatement(
                     "insert into emergency_contact (user_id, name, phone_number) values (?, '친구', '01087654321')")) {
            insert.setLong(1, id);
            assertThatThrownBy(insert::executeUpdate).isInstanceOf(SQLException.class);
        }
    }

    @Test
    void blocksAccessWhenACompletedUsersProfileIsInactive() throws Exception {
        Long id = user(UserOnboardingStatus.PHONE_VERIFIED).id();
        registerProfile.register(new RegisterCompanionProfileCommand(
                id, "비활성" + id, UserGender.FEMALE, null, null, List.of(TravelStyleKeyword.EXTROVERTED)));
        CompanionProfile profile = profiles.findByUserId(id).orElseThrow();
        profiles.save(new CompanionProfile(profile.id(), id, profile.nickname(), profile.gender(),
                profile.birthYear(), profile.profileImageUrl(), profile.intro(), profile.mannerScore(),
                profile.reviewCount(), CompanionProfileStatus.INACTIVE));

        mvc.perform(get("/api/companion-matches").with(jwt().jwt(token -> token.subject(id.toString()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANION_PROFILE_REQUIRED"));
    }

    @Test
    void documentsNewEndpointsAndCompanionProfileRequirement() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/onboarding'].get").exists())
                .andExpect(jsonPath("$.paths['/api/onboarding/emergency-contact'].put").exists())
                .andExpect(jsonPath("$.paths['/api/onboarding/companion-profiles/skip'].post").exists())
                .andExpect(jsonPath("$.paths['/api/companion-matches'].get.responses['403']"
                        + ".content['application/json'].examples.COMPANION_PROFILE_REQUIRED.value.code")
                        .value("COMPANION_PROFILE_REQUIRED"));
        for (String path : List.of("/api/companion-posts", "/api/companion-profiles/{profileId}",
                "/api/companion-requests/{applicationId}/review", "/api/companion-meetings",
                "/api/users/me/recruitment-posts", "/api/users/me/companion-requests/page",
                "/api/users/me/companion-requests/{applicationId}/result")) {
            mvc.perform(get("/v3/api-docs"))
                    .andExpect(jsonPath("$.paths['" + path + "'].get.responses['403']"
                            + ".content['application/json'].examples.COMPANION_PROFILE_REQUIRED.value.code")
                            .value("COMPANION_PROFILE_REQUIRED"));
        }
    }

    private UserAccount user(final UserOnboardingStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 0, 0);
        return users.save(new UserAccount(null, UserRole.USER, UserAccountStatus.ACTIVE,
                status == UserOnboardingStatus.STARTED ? null : "01012345678",
                status == UserOnboardingStatus.STARTED ? null : now, status, now, null));
    }

    private int countContacts(final Long userId) throws Exception {
        try (var connection = dataSource.getConnection();
             var query = connection.prepareStatement("select count(*) from emergency_contact where user_id = ?")) {
            query.setLong(1, userId);
            try (var result = query.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private void concurrently(final List<Runnable> requests) throws Exception {
        try (var executor = Executors.newFixedThreadPool(requests.size())) {
            CountDownLatch ready = new CountDownLatch(requests.size());
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> results = new ArrayList<>();
            for (Runnable request : requests) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("동시 요청 시작 대기 시간 초과");
                        }
                        request.run();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                }));
            }
            try {
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            for (Future<?> result : results) {
                result.get(15, TimeUnit.SECONDS);
            }
        }
    }
}
