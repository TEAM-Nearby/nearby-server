// 현재 온보딩 진행 상태와 선택 정보 등록 여부를 공개하는 유스케이스다.
package com.sopt.nearby.user.port.in;

public interface ReadOnboardingStatusUseCase {
    Status read(Long userId);

    record Status(String onboardingStatus, boolean phoneVerified,
                  boolean hasCompanionProfile, boolean hasEmergencyContact) {
    }
}
