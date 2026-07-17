// 시간 의존 컴포넌트가 공통 Clock을 주입받는지 검증한다.
package com.sopt.nearby.shared.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sopt.nearby.user.adapter.out.persistence.RefreshTokenRepositoryAdapter;
import com.sopt.nearby.user.adapter.out.security.JwtTokenAdapter;
import com.sopt.nearby.user.application.ConfirmPhoneVerificationCodeService;
import com.sopt.nearby.user.application.KakaoLoginService;
import com.sopt.nearby.user.application.LogoutUserService;
import com.sopt.nearby.user.application.RefreshTokenService;
import com.sopt.nearby.user.application.SendPhoneVerificationCodeService;
import java.time.Clock;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class KoreaTimeClockInjectionTest {

    @Test
    void injectsCommonClockIntoTimeDependentComponents() {
        assertClockParameter(KakaoLoginService.class);
        assertClockParameter(SendPhoneVerificationCodeService.class);
        assertClockParameter(ConfirmPhoneVerificationCodeService.class);
        assertClockParameter(RefreshTokenService.class);
        assertClockParameter(LogoutUserService.class);
        assertClockParameter(RefreshTokenRepositoryAdapter.class);
        assertClockParameter(JwtTokenAdapter.class);
    }

    private void assertClockParameter(final Class<?> componentType) {
        assertTrue(Arrays.stream(componentType.getConstructors())
                .allMatch(constructor -> Arrays.asList(constructor.getParameterTypes()).contains(Clock.class)));
    }
}
