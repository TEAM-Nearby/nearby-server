// 도메인 온보딩 상태를 기존 API 공개 값으로 변환한다.
package com.sopt.nearby.user.adapter.in.web.dto.response;

import com.sopt.nearby.user.domain.model.UserOnboardingStatus;

final class OnboardingStatusMapper {
    private OnboardingStatusMapper() {
    }

    static String toApiStatus(final UserOnboardingStatus status) {
        return switch (status) {
            case STARTED, TERMS_AGREED -> "STARTED";
            case PHONE_VERIFIED -> "PHONE_VERIFIED";
            case COMPANION_PROFILE_COMPLETED, COMPANION_PROFILE_SKIPPED, COMPLETED -> "COMPLETED";
        };
    }
}
