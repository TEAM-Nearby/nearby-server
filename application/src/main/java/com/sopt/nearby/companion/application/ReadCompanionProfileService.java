// 동행 프로필 상세 조회 유스케이스를 구현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileDetail;
import com.sopt.nearby.companion.port.in.ReadCompanionProfileUseCase;
import com.sopt.nearby.companion.port.out.CompanionProfileDetailQueryPort;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import org.springframework.transaction.annotation.Transactional;

public class ReadCompanionProfileService implements ReadCompanionProfileUseCase {

    private final CompanionProfileDetailQueryPort queryPort;
    private final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase;

    public ReadCompanionProfileService(
            final CompanionProfileDetailQueryPort queryPort,
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase
    ) {
        this.queryPort = queryPort;
        this.requireCompletedOnboardingUseCase = requireCompletedOnboardingUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanionProfileDetail read(final ReadCompanionProfileCommand command) {
        if (command == null
                || command.viewerUserId() == null
                || command.viewerUserId() <= 0
                || command.profileId() == null
                || command.profileId() <= 0) {
            throw new CompanionProfileNotFoundException();
        }

        requireCompletedOnboardingUseCase.requireCompleted(command.viewerUserId());
        return queryPort.findByProfileId(command.profileId())
                .orElseThrow(CompanionProfileNotFoundException::new);
    }
}
