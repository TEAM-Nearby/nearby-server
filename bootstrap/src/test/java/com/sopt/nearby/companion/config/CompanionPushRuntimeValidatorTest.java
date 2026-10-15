// 동행 푸시 워커의 프로필과 lease·타임아웃 설정 검증을 확인한다.
package com.sopt.nearby.companion.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class CompanionPushRuntimeValidatorTest {

    @Test
    void rejectsWorkerWithoutFcmProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("supabase");

        assertThrows(IllegalStateException.class, () -> new CompanionPushRuntimeValidator(
                environment,
                true,
                "nearby-project",
                30,
                5_000,
                10_000,
                10_000,
                5
        ));
    }

    @Test
    void rejectsLeaseNotLongerThanProviderTimeoutBudgetAndMargin() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("fcm");

        assertThrows(IllegalStateException.class, () -> new CompanionPushRuntimeValidator(
                environment,
                true,
                "nearby-project",
                30,
                5_000,
                10_000,
                10_000,
                5
        ));
    }

    @Test
    void acceptsFcmProfileWithValidLeaseConfiguration() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("fcm");

        assertDoesNotThrow(() -> new CompanionPushRuntimeValidator(
                environment,
                true,
                "nearby-project",
                31,
                5_000,
                10_000,
                10_000,
                5
        ));
    }

    @Test
    void acceptsLocalProfileForDevelopmentWorker() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        assertDoesNotThrow(() -> new CompanionPushRuntimeValidator(
                environment,
                true,
                "",
                31,
                5_000,
                10_000,
                10_000,
                5
        ));
    }
}
