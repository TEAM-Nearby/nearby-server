// 온보딩 완료가 필요한 API를 호출했을 때 발생하는 예외다.
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class OnboardingRequiredException extends BusinessException {

    public OnboardingRequiredException() {
        super(OnboardingErrorCode.ONBOARDING_REQUIRED);
    }
}
