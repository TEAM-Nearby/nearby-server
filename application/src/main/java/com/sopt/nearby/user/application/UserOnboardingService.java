// 온보딩 상태 조회와 동행 프로필 건너뛰기를 처리한다.
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.exception.PhoneVerificationRequiredException;
import com.sopt.nearby.user.exception.UserNotFoundException;
import com.sopt.nearby.user.port.in.ReadOnboardingStatusUseCase;
import com.sopt.nearby.user.port.in.SkipCompanionProfileUseCase;
import com.sopt.nearby.user.port.out.EmergencyContactRepository;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserOnboardingService implements ReadOnboardingStatusUseCase, SkipCompanionProfileUseCase {
    private final UserAccountRepository users;
    private final EmergencyContactRepository contacts;

    public UserOnboardingService(final UserAccountRepository users, final EmergencyContactRepository contacts) {
        this.users = users;
        this.contacts = contacts;
    }

    @Override
    @Transactional(readOnly = true)
    public Status read(final Long userId) {
        return status(users.findById(userId).orElseThrow(UserNotFoundException::new));
    }

    @Override
    @Transactional
    public Status skip(final Long userId) {
        UserAccount user = users.findByIdForUpdate(userId).orElseThrow(UserNotFoundException::new);
        if (user.onboardingStatus().isCompleted()) {
            return status(user);
        }
        if (user.phoneVerifiedAt() == null || user.onboardingStatus() != UserOnboardingStatus.PHONE_VERIFIED) {
            throw new PhoneVerificationRequiredException();
        }
        return status(users.save(new UserAccount(
                user.id(), user.role(), user.status(), user.phoneNumber(), user.phoneVerifiedAt(),
                UserOnboardingStatus.COMPANION_PROFILE_SKIPPED, user.createdAt(), user.deletedAt()
        )));
    }

    private Status status(final UserAccount user) {
        return new Status(user.onboardingStatus(), user.phoneVerifiedAt() != null,
                user.onboardingStatus().hasCompanionProfile(), contacts.findByUserId(user.id()).isPresent());
    }
}
