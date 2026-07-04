// 휴대폰 인증 문자 발송 유스케이스의 저장, 발송, 예외 동작을 검증하는 테스트
package com.sopt.nearby.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.user.domain.model.PhoneVerification;
import com.sopt.nearby.user.domain.model.PhoneVerificationStatus;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.exception.PhoneVerificationSendLimitExceededException;
import com.sopt.nearby.user.exception.UserNotFoundException;
import com.sopt.nearby.user.port.out.PhoneVerificationRepository;
import com.sopt.nearby.user.port.out.PhoneVerificationSender;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class SendPhoneVerificationCodeServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-04T07:00:00Z"), ZoneId.of("UTC"));
	private static final String HASH_SECRET = "nearby-phone-verification-test-secret-32bytes";
	private static final String CODE_123456_HASH =
			"2a0ce34b1c06ef8464a8b86091d1fbfb8ea80db5fae72670fbeebb6ec17ee395";

	@Test
	void savesPendingVerificationAndSendsCode() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		userAccounts.save(newUser(1L));
		FakePhoneVerificationRepository phoneVerifications = new FakePhoneVerificationRepository();
		FakePhoneVerificationSender sender = new FakePhoneVerificationSender();
		SendPhoneVerificationCodeService service = new SendPhoneVerificationCodeService(
				userAccounts,
				phoneVerifications,
				sender,
				HASH_SECRET,
				CLOCK,
				() -> 123456
		);

		SendPhoneVerificationCodeResult result = service.send(
				new SendPhoneVerificationCodeCommand(1L, "01012345678")
		);

		assertEquals(1L, result.phoneVerificationId());
		assertEquals(180, result.expiresIn());
		PhoneVerification saved = phoneVerifications.saved.get(1L);
		assertEquals(1L, saved.userId());
		assertEquals("01012345678", saved.phoneNumber());
		assertEquals(PhoneVerificationStatus.PENDING, saved.status());
		assertEquals(LocalDateTime.of(2026, 7, 4, 7, 3), saved.expiresAt());
		assertEquals(CODE_123456_HASH, saved.verificationCodeHash());
		assertEquals("01012345678", sender.phoneNumber);
		assertEquals("123456", sender.verificationCode);
	}

	@Test
	void propagatesSendLimitExceededWhenSenderRejects() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		userAccounts.save(newUser(1L));
		SendPhoneVerificationCodeService service = new SendPhoneVerificationCodeService(
				userAccounts,
				new FakePhoneVerificationRepository(),
				(phoneNumber, verificationCode) -> {
					throw new PhoneVerificationSendLimitExceededException();
				},
				HASH_SECRET,
				CLOCK,
				() -> 123456
		);

		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> service.send(new SendPhoneVerificationCodeCommand(1L, "01012345678"))
		);

		assertInstanceOf(PhoneVerificationSendLimitExceededException.class, exception);
	}

	@Test
	void failsWhenUserDoesNotExist() {
		FakePhoneVerificationSender sender = new FakePhoneVerificationSender();
		SendPhoneVerificationCodeService service = new SendPhoneVerificationCodeService(
				new FakeUserAccountRepository(),
				new FakePhoneVerificationRepository(),
				sender,
				HASH_SECRET,
				CLOCK,
				() -> 123456
		);

		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> service.send(new SendPhoneVerificationCodeCommand(99L, "01012345678"))
		);

		assertInstanceOf(UserNotFoundException.class, exception);
		assertEquals(null, sender.verificationCode);
	}

	@Test
	void sendMethodDoesNotHoldTransactionWhileSendingSms() throws NoSuchMethodException {
		assertEquals(
				null,
				SendPhoneVerificationCodeService.class
						.getMethod("send", SendPhoneVerificationCodeCommand.class)
						.getAnnotation(Transactional.class)
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

		@Override
		public UserAccount save(final UserAccount model) {
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
		private long nextId = 1L;

		@Override
		public PhoneVerification save(final PhoneVerification model) {
			PhoneVerification savedVerification = new PhoneVerification(
					model.id() == null ? nextId++ : model.id(),
					model.userId(),
					model.phoneNumber(),
					model.carrier(),
					model.verificationCodeHash(),
					model.status(),
					model.expiresAt(),
					model.verifiedAt()
			);
			saved.put(savedVerification.id(), savedVerification);
			return savedVerification;
		}

		@Override
		public Optional<PhoneVerification> findById(final Long id) {
			return Optional.ofNullable(saved.get(id));
		}
	}

	private static final class FakePhoneVerificationSender implements PhoneVerificationSender {

		private String phoneNumber;
		private String verificationCode;

		@Override
		public void send(final String phoneNumber, final String verificationCode) {
			this.phoneNumber = phoneNumber;
			this.verificationCode = verificationCode;
		}
	}
}
