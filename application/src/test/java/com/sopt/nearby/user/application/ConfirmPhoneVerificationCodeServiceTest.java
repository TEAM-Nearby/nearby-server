// 휴대폰 인증 번호 확인 유스케이스의 검증과 상태 갱신을 검증하는 테스트
package com.sopt.nearby.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.user.domain.model.PhoneVerification;
import com.sopt.nearby.user.domain.model.PhoneVerificationStatus;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.exception.PhoneVerificationCodeMismatchException;
import com.sopt.nearby.user.exception.PhoneVerificationExpiredException;
import com.sopt.nearby.user.exception.PhoneVerificationNotFoundException;
import com.sopt.nearby.user.port.out.PhoneVerificationCodeStore;
import com.sopt.nearby.user.port.out.PhoneVerificationRepository;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ConfirmPhoneVerificationCodeServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-04T07:00:00Z"), ZoneOffset.UTC);
	private static final String HASH_SECRET = "nearby-phone-verification-test-secret-32bytes";
	private static final String CODE_123456_HASH =
			"2a0ce34b1c06ef8464a8b86091d1fbfb8ea80db5fae72670fbeebb6ec17ee395";

	@Test
	void confirmsCodeAndUpdatesPhoneVerificationAndUserAccount() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		userAccounts.save(newUser(1L));
		FakePhoneVerificationRepository phoneVerifications = new FakePhoneVerificationRepository();
		phoneVerifications.save(newPendingVerification(10L, 1L, "01012345678"));
		FakePhoneVerificationCodeStore codeStore = new FakePhoneVerificationCodeStore();
		codeStore.save(10L, CODE_123456_HASH, Duration.ofSeconds(180));
		ConfirmPhoneVerificationCodeService service = new ConfirmPhoneVerificationCodeService(
				userAccounts,
				phoneVerifications,
				codeStore,
				HASH_SECRET,
				CLOCK
		);

		ConfirmPhoneVerificationCodeResult result = service.confirm(
				new ConfirmPhoneVerificationCodeCommand(1L, 10L, "123456")
		);

		assertEquals(true, result.phoneVerified());
		assertEquals(UserOnboardingStatus.PHONE_VERIFIED, result.onboardingStatus());
		PhoneVerification verified = phoneVerifications.saved.get(10L);
		assertEquals(PhoneVerificationStatus.VERIFIED, verified.status());
		assertEquals(LocalDateTime.of(2026, 7, 4, 7, 0), verified.verifiedAt());
		UserAccount updatedUser = userAccounts.accounts.get(1L);
		assertEquals("01012345678", updatedUser.phoneNumber());
		assertEquals(LocalDateTime.of(2026, 7, 4, 7, 0), updatedUser.phoneVerifiedAt());
		assertEquals(UserOnboardingStatus.PHONE_VERIFIED, updatedUser.onboardingStatus());
		assertEquals(false, codeStore.findHash(10L).isPresent());
	}

	@Test
	void failsWhenVerificationCodeDoesNotMatch() {
		ConfirmPhoneVerificationCodeService service = newServiceWithPendingVerification();

		assertThrows(
				PhoneVerificationCodeMismatchException.class,
				() -> service.confirm(new ConfirmPhoneVerificationCodeCommand(1L, 10L, "000000"))
		);
		assertThrows(
				PhoneVerificationCodeMismatchException.class,
				() -> service.confirm(new ConfirmPhoneVerificationCodeCommand(1L, 10L, ""))
		);
		assertThrows(
				PhoneVerificationCodeMismatchException.class,
				() -> service.confirm(new ConfirmPhoneVerificationCodeCommand(1L, 10L, null))
		);
		assertThrows(
				PhoneVerificationCodeMismatchException.class,
				() -> service.confirm(new ConfirmPhoneVerificationCodeCommand(1L, 10L, "12345"))
		);
		assertThrows(
				PhoneVerificationCodeMismatchException.class,
				() -> service.confirm(new ConfirmPhoneVerificationCodeCommand(1L, 10L, "abcdef"))
		);
	}

	@Test
	void keepsCodeHashWhenUserAccountUpdateFails() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		userAccounts.save(newUser(1L));
		FakePhoneVerificationRepository phoneVerifications = new FakePhoneVerificationRepository();
		phoneVerifications.save(newPendingVerification(10L, 1L, "01012345678"));
		FakePhoneVerificationCodeStore codeStore = new FakePhoneVerificationCodeStore();
		codeStore.save(10L, CODE_123456_HASH, Duration.ofSeconds(180));
		ConfirmPhoneVerificationCodeService service = new ConfirmPhoneVerificationCodeService(
				userAccounts,
				phoneVerifications,
				codeStore,
				HASH_SECRET,
				CLOCK
		);
		userAccounts.failOnSave = true;

		assertThrows(
				IllegalStateException.class,
				() -> service.confirm(new ConfirmPhoneVerificationCodeCommand(1L, 10L, "123456"))
		);
		assertEquals(true, codeStore.findHash(10L).isPresent());
	}

	@Test
	void failsWhenCodeHashIsMissingFromStoreButVerificationIsNotExpired() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		userAccounts.save(newUser(1L));
		FakePhoneVerificationRepository phoneVerifications = new FakePhoneVerificationRepository();
		phoneVerifications.save(newPendingVerification(10L, 1L, "01012345678"));
		ConfirmPhoneVerificationCodeService service = new ConfirmPhoneVerificationCodeService(
				userAccounts,
				phoneVerifications,
				new FakePhoneVerificationCodeStore(),
				HASH_SECRET,
				CLOCK
		);

		assertThrows(
				PhoneVerificationExpiredException.class,
				() -> service.confirm(new ConfirmPhoneVerificationCodeCommand(1L, 10L, "123456"))
		);
	}

	@Test
	void deletesCodeOnlyAfterCommitWhenSynchronizationIsActive() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		userAccounts.save(newUser(1L));
		FakePhoneVerificationRepository phoneVerifications = new FakePhoneVerificationRepository();
		phoneVerifications.save(newPendingVerification(10L, 1L, "01012345678"));
		FakePhoneVerificationCodeStore codeStore = new FakePhoneVerificationCodeStore();
		codeStore.save(10L, CODE_123456_HASH, Duration.ofSeconds(180));
		ConfirmPhoneVerificationCodeService service = new ConfirmPhoneVerificationCodeService(
				userAccounts,
				phoneVerifications,
				codeStore,
				HASH_SECRET,
				CLOCK
		);

		TransactionSynchronizationManager.initSynchronization();
		try {
			service.confirm(new ConfirmPhoneVerificationCodeCommand(1L, 10L, "123456"));

			assertEquals(true, codeStore.findHash(10L).isPresent());
			assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
			TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();
			assertEquals(false, codeStore.findHash(10L).isPresent());
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void failsWhenVerificationDoesNotExist() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		userAccounts.save(newUser(1L));
		ConfirmPhoneVerificationCodeService service = new ConfirmPhoneVerificationCodeService(
				userAccounts,
				new FakePhoneVerificationRepository(),
				new FakePhoneVerificationCodeStore(),
				HASH_SECRET,
				CLOCK
		);

		assertThrows(
				PhoneVerificationNotFoundException.class,
				() -> service.confirm(new ConfirmPhoneVerificationCodeCommand(1L, 99L, "123456"))
		);
	}

	@Test
	void failsWhenVerificationBelongsToAnotherUser() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		userAccounts.save(newUser(1L));
		FakePhoneVerificationRepository phoneVerifications = new FakePhoneVerificationRepository();
		phoneVerifications.save(newPendingVerification(10L, 2L, "01012345678"));
		ConfirmPhoneVerificationCodeService service = new ConfirmPhoneVerificationCodeService(
				userAccounts,
				phoneVerifications,
				new FakePhoneVerificationCodeStore(),
				HASH_SECRET,
				CLOCK
		);

		assertThrows(
				PhoneVerificationNotFoundException.class,
				() -> service.confirm(new ConfirmPhoneVerificationCodeCommand(1L, 10L, "123456"))
		);
	}

	@Test
	void failsWhenVerificationExpired() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		userAccounts.save(newUser(1L));
		FakePhoneVerificationRepository phoneVerifications = new FakePhoneVerificationRepository();
		phoneVerifications.save(new PhoneVerification(
				10L,
				1L,
				"01012345678",
				null,
				CODE_123456_HASH,
				PhoneVerificationStatus.PENDING,
				LocalDateTime.of(2026, 7, 4, 6, 59, 59),
				null
		));
		FakePhoneVerificationCodeStore codeStore = new FakePhoneVerificationCodeStore();
		codeStore.save(10L, CODE_123456_HASH, Duration.ofSeconds(180));
		ConfirmPhoneVerificationCodeService service = new ConfirmPhoneVerificationCodeService(
				userAccounts,
				phoneVerifications,
				codeStore,
				HASH_SECRET,
				CLOCK
		);

		assertThrows(
				PhoneVerificationExpiredException.class,
				() -> service.confirm(new ConfirmPhoneVerificationCodeCommand(1L, 10L, "123456"))
		);
	}

	@Test
	void failsWhenVerificationIsAlreadyVerified() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		userAccounts.save(newUser(1L));
		FakePhoneVerificationRepository phoneVerifications = new FakePhoneVerificationRepository();
		phoneVerifications.save(new PhoneVerification(
				10L,
				1L,
				"01012345678",
				null,
				CODE_123456_HASH,
				PhoneVerificationStatus.VERIFIED,
				LocalDateTime.of(2026, 7, 4, 7, 3),
				LocalDateTime.of(2026, 7, 4, 6, 59)
		));
		ConfirmPhoneVerificationCodeService service = new ConfirmPhoneVerificationCodeService(
				userAccounts,
				phoneVerifications,
				new FakePhoneVerificationCodeStore(),
				HASH_SECRET,
				CLOCK
		);

		assertThrows(
				PhoneVerificationCodeMismatchException.class,
				() -> service.confirm(new ConfirmPhoneVerificationCodeCommand(1L, 10L, "123456"))
		);
		assertEquals(LocalDateTime.of(2026, 7, 4, 6, 59), phoneVerifications.saved.get(10L).verifiedAt());
	}

	private ConfirmPhoneVerificationCodeService newServiceWithPendingVerification() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		userAccounts.save(newUser(1L));
		FakePhoneVerificationRepository phoneVerifications = new FakePhoneVerificationRepository();
		phoneVerifications.save(newPendingVerification(10L, 1L, "01012345678"));
		FakePhoneVerificationCodeStore codeStore = new FakePhoneVerificationCodeStore();
		codeStore.save(10L, CODE_123456_HASH, Duration.ofSeconds(180));
		return new ConfirmPhoneVerificationCodeService(userAccounts, phoneVerifications, codeStore, HASH_SECRET, CLOCK);
	}

	private static PhoneVerification newPendingVerification(
			final Long id,
			final Long userId,
			final String phoneNumber
	) {
		return new PhoneVerification(
				id,
				userId,
				phoneNumber,
				null,
				CODE_123456_HASH,
				PhoneVerificationStatus.PENDING,
				LocalDateTime.of(2026, 7, 4, 7, 3),
				null
		);
	}

	private static UserAccount newUser(final Long id) {
		return new UserAccount(
				id,
				UserRole.USER,
				UserAccountStatus.ACTIVE,
				null,
				null,
				UserOnboardingStatus.STARTED,
				LocalDateTime.of(2026, 7, 4, 7, 0),
				null
		);
	}

	private static final class FakeUserAccountRepository implements UserAccountRepository {

		private final Map<Long, UserAccount> accounts = new HashMap<>();
		private boolean failOnSave;

		@Override
		public UserAccount save(final UserAccount model) {
			if (failOnSave) {
				throw new IllegalStateException("failed to save user account");
			}
			accounts.put(model.id(), model);
			return model;
		}

		@Override
		public Optional<UserAccount> findById(final Long id) {
			return Optional.ofNullable(accounts.get(id));
		}
	}

	private static final class FakePhoneVerificationRepository implements PhoneVerificationRepository {

		private final Map<Long, PhoneVerification> saved = new HashMap<>();

		@Override
		public PhoneVerification save(final PhoneVerification model) {
			saved.put(model.id(), model);
			return model;
		}

		@Override
		public Optional<PhoneVerification> findById(final Long id) {
			return Optional.ofNullable(saved.get(id));
		}
	}

	private static final class FakePhoneVerificationCodeStore implements PhoneVerificationCodeStore {

		private final Map<Long, String> codeHashes = new HashMap<>();

		@Override
		public void save(final Long phoneVerificationId, final String verificationCodeHash, final Duration ttl) {
			codeHashes.put(phoneVerificationId, verificationCodeHash);
		}

		@Override
		public Optional<String> findHash(final Long phoneVerificationId) {
			return Optional.ofNullable(codeHashes.get(phoneVerificationId));
		}

		@Override
		public void delete(final Long phoneVerificationId) {
			codeHashes.remove(phoneVerificationId);
		}
	}
}
