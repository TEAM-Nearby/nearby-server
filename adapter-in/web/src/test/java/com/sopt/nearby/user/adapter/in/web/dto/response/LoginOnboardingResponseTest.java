// 소셜 로그인 응답에서 온보딩 완료와 프로필 등록 여부를 구분하는지 검증한다.
package com.sopt.nearby.user.adapter.in.web.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.user.application.AppleLoginResult;
import com.sopt.nearby.user.application.KakaoLoginResult;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LoginOnboardingResponseTest {
    @ParameterizedTest
    @CsvSource({
            "STARTED, STARTED, false",
            "TERMS_AGREED, STARTED, false",
            "PHONE_VERIFIED, PHONE_VERIFIED, false",
            "COMPANION_PROFILE_SKIPPED, COMPLETED, false",
            "COMPANION_PROFILE_COMPLETED, COMPLETED, true",
            "COMPLETED, COMPLETED, true"
    })
    void exposesProfileFlagWithoutChangingExistingStatusContract(
            final UserOnboardingStatus storedStatus, final String apiStatus, final boolean hasProfile
    ) {
        var apple = AppleLoginResponse.from(new AppleLoginResult(
                "access", "refresh", "Bearer", 3600, 1209600, 1L, storedStatus));
        var kakao = KakaoLoginResponse.from(new KakaoLoginResult(
                "access", "refresh", "Bearer", 3600, 1209600, 1L, storedStatus));

        assertThat(apple.onboardingStatus()).isEqualTo(apiStatus);
        assertThat(kakao.onboardingStatus()).isEqualTo(apiStatus);
        assertThat(apple.hasCompanionProfile()).isEqualTo(hasProfile);
        assertThat(kakao.hasCompanionProfile()).isEqualTo(hasProfile);
    }
}
