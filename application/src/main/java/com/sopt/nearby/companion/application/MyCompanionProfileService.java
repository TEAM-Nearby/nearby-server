// 본인 프로필을 조회하고 수정 항목과 여행 스타일을 함께 갱신한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionProfileRequiredException;
import com.sopt.nearby.companion.domain.exception.DuplicateNicknameException;
import com.sopt.nearby.companion.domain.exception.InactiveCompanionProfileException;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStyle;
import com.sopt.nearby.companion.port.in.ReadMyCompanionProfileUseCase;
import com.sopt.nearby.companion.port.in.UpdateMyCompanionProfileUseCase;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileStyleRepository;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public class MyCompanionProfileService implements ReadMyCompanionProfileUseCase, UpdateMyCompanionProfileUseCase {
    private final CompanionProfileRepository profiles;
    private final CompanionProfileStyleRepository styles;
    private final RequireCompletedOnboardingUseCase onboarding;

    public MyCompanionProfileService(final CompanionProfileRepository profiles,
                                     final CompanionProfileStyleRepository styles,
                                     final RequireCompletedOnboardingUseCase onboarding) {
        this.profiles = profiles;
        this.styles = styles;
        this.onboarding = onboarding;
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public MyCompanionProfileResult read(final Long userId) {
        onboarding.requireCompleted(userId);
        CompanionProfile profile = requireActive(profiles.findByUserId(userId)
                .orElseThrow(CompanionProfileRequiredException::new));
        return MyCompanionProfileResult.from(profile, styles.findAllByProfileId(profile.id()).stream()
                .map(CompanionProfileStyle::keyword).toList());
    }

    @Override
    @Transactional
    public MyCompanionProfileResult update(final UpdateMyCompanionProfileCommand command) {
        onboarding.requireCompleted(command.userId());
        CompanionProfile profile = requireActive(profiles.findByUserIdForUpdate(command.userId())
                .orElseThrow(CompanionProfileRequiredException::new));
        if (!profile.nickname().equals(command.nickname()) && profiles.existsByNickname(command.nickname())) {
            throw new DuplicateNicknameException();
        }

        CompanionProfile updated = profiles.save(new CompanionProfile(
                profile.id(), profile.userId(), command.nickname(), profile.gender(), profile.birthYear(),
                command.profileImageUrl(), command.intro(), profile.mannerScore(), profile.reviewCount(), profile.status()));
        styles.deleteAllByProfileId(profile.id());
        command.travelStyleKeywords().forEach(keyword -> styles.save(new CompanionProfileStyle(profile.id(), keyword)));
        return MyCompanionProfileResult.from(updated, command.travelStyleKeywords());
    }

    private CompanionProfile requireActive(final CompanionProfile profile) {
        if (profile.status() != CompanionProfileStatus.ACTIVE) {
            throw new InactiveCompanionProfileException();
        }
        return profile;
    }
}
