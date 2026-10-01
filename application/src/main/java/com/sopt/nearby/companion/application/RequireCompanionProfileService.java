// 온보딩 완료와 활성 동행 프로필을 확인한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionProfileRequiredException;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.port.in.RequireCompanionProfileUseCase;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequireCompanionProfileService implements RequireCompanionProfileUseCase {
    private final RequireCompletedOnboardingUseCase onboarding;
    private final CompanionProfileRepository profiles;

    public RequireCompanionProfileService(final RequireCompletedOnboardingUseCase onboarding,
                                           final CompanionProfileRepository profiles) {
        this.onboarding = onboarding;
        this.profiles = profiles;
    }

    @Override
    @Transactional(readOnly = true)
    public void requireProfile(final Long userId) {
        onboarding.requireCompleted(userId);
        profiles.findByUserId(userId)
                .filter(profile -> profile.status() == CompanionProfileStatus.ACTIVE)
                .orElseThrow(CompanionProfileRequiredException::new);
    }
}
