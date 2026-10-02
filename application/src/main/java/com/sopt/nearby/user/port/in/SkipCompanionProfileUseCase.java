// 동행 프로필 없이 온보딩을 마치는 유스케이스다.
package com.sopt.nearby.user.port.in;

public interface SkipCompanionProfileUseCase {
    ReadOnboardingStatusUseCase.Status skip(Long userId);
}
