// 온보딩 진행 상태와 선택 정보 등록 여부를 API 응답으로 표현한다.
package com.sopt.nearby.user.adapter.in.web.dto.response;

import com.sopt.nearby.user.port.in.ReadOnboardingStatusUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

public record OnboardingStatusResponse(
        @Schema(allowableValues = {"STARTED", "PHONE_VERIFIED", "COMPLETED"}) String onboardingStatus,
        @Schema(description = "휴대폰 인증 완료 여부") boolean phoneVerified,
        @Schema(description = "동행 프로필 등록 완료 여부. false이면 동행 화면 진입 시 등록을 안내합니다.") boolean hasCompanionProfile,
        @Schema(description = "선택 사항인 비상 연락망 등록 여부") boolean hasEmergencyContact
) {
    public static OnboardingStatusResponse from(final ReadOnboardingStatusUseCase.Status status) {
        return new OnboardingStatusResponse(OnboardingStatusMapper.toApiStatus(status.onboardingStatus()), status.phoneVerified(),
                status.hasCompanionProfile(), status.hasEmergencyContact());
    }
}
