// 동행 프로필 등록 후 회원 온보딩 완료 처리를 검증하는 테스트
package com.sopt.nearby.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.exception.PhoneVerificationRequiredException;
import com.sopt.nearby.user.exception.UserNotFoundException;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CompleteCompanionProfileOnboardingServiceTest {

	private final FakeUserAccountRepository userAccountRepository = new FakeUserAccountRepository();
	private final CompleteCompanionProfileOnboardingService service = new CompleteCompanionProfileOnboardingService(
			userAccountRepository
	);

	@Test
	void completesOnboardingWhenPhoneIsVerified() {
		userAccountRepository.userAccount = user(UserOnboardingStatus.PHONE_VERIFIED);

		String result = service.complete(1L);

		assertEquals("COMPLETED", result);
		assertEquals(UserOnboardingStatus.COMPLETED, userAccountRepository.savedUserAccount.onboardingStatus());
		assertEquals("01012345678", userAccountRepository.savedUserAccount.phoneNumber());
	}

	@Test
	void rejectsUserWithoutPhoneVerification() {
		userAccountRepository.userAccount = user(UserOnboardingStatus.STARTED);

		assertThrows(PhoneVerificationRequiredException.class, () -> service.complete(1L));
		assertEquals(null, userAccountRepository.savedUserAccount);
	}

	@Test
	void rejectsMissingUser() {
		assertThrows(UserNotFoundException.class, () -> service.complete(1L));
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

	static class FakeUserAccountRepository implements UserAccountRepository {

		private UserAccount userAccount;
		private UserAccount savedUserAccount;

		@Override
		public UserAccount save(final UserAccount model) {
			savedUserAccount = model;
			userAccount = model;
			return model;
		}

		@Override
		public Optional<UserAccount> findById(final Long id) {
			return Optional.ofNullable(userAccount);
		}
	}
}

