// 회원 온보딩 완료 여부 확인 유스케이스를 구현한다.
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.exception.OnboardingRequiredException;
import com.sopt.nearby.user.exception.UserNotFoundException;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequireCompletedOnboardingService implements RequireCompletedOnboardingUseCase {

    private final UserAccountRepository userAccountRepository;

    public RequireCompletedOnboardingService(final UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void requireCompleted(final Long userId) {
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (!isCompleted(userAccount.onboardingStatus())) {
            throw new OnboardingRequiredException();
        }
    }

    private boolean isCompleted(final UserOnboardingStatus status) {
        return switch (status) {
            case COMPLETED, COMPANION_PROFILE_COMPLETED, COMPANION_PROFILE_SKIPPED -> true;
            default -> false;
        };
    }
}
