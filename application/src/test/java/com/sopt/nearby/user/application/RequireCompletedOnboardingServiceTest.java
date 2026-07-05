// 회원 온보딩 완료 여부 확인 서비스를 검증한다.
package com.sopt.nearby.user.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.exception.OnboardingRequiredException;
import com.sopt.nearby.user.exception.UserNotFoundException;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RequireCompletedOnboardingServiceTest {

    private final FakeUserAccountRepository userAccountRepository = new FakeUserAccountRepository();
    private final RequireCompletedOnboardingService service = new RequireCompletedOnboardingService(userAccountRepository);

    @Test
    void allowsCompletedUser() {
        userAccountRepository.userAccount = user(UserOnboardingStatus.COMPLETED);

        assertDoesNotThrow(() -> service.requireCompleted(1L));
    }

    @Test
    void rejectsIncompleteUser() {
        userAccountRepository.userAccount = user(UserOnboardingStatus.PHONE_VERIFIED);

        assertThrows(OnboardingRequiredException.class, () -> service.requireCompleted(1L));
    }

    @Test
    void rejectsMissingUser() {
        assertThrows(UserNotFoundException.class, () -> service.requireCompleted(1L));
    }

    private static UserAccount user(final UserOnboardingStatus onboardingStatus) {
        return new UserAccount(
                1L,
                UserRole.USER,
                UserAccountStatus.ACTIVE,
                "01012345678",
                LocalDateTime.of(2026, 7, 5, 7, 0),
                onboardingStatus,
                LocalDateTime.of(2026, 7, 5, 6, 0),
                null
        );
    }

    private static final class FakeUserAccountRepository implements UserAccountRepository {

        private UserAccount userAccount;

        @Override
        public UserAccount save(final UserAccount model) {
            userAccount = model;
            return model;
        }

        @Override
        public Optional<UserAccount> findById(final Long id) {
            return Optional.ofNullable(userAccount);
        }
    }
}
