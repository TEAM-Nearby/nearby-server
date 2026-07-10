// 휴대폰 인증 코드를 생성, 저장하고 문자 발송 포트로 전달하는 유스케이스
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.PhoneVerification;
import com.sopt.nearby.user.domain.model.PhoneVerificationStatus;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.exception.UserNotFoundException;
import com.sopt.nearby.user.port.in.SendPhoneVerificationCodeUseCase;
import com.sopt.nearby.user.port.out.PhoneVerificationCodeStore;
import com.sopt.nearby.user.port.out.PhoneVerificationRepository;
import com.sopt.nearby.user.port.out.PhoneVerificationSender;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.function.IntSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SendPhoneVerificationCodeService implements SendPhoneVerificationCodeUseCase {

	private static final int EXPIRES_IN_SECONDS = 180;
	private static final int CODE_BOUND = 1_000_000;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserAccountRepository userAccountRepository;
	private final PhoneVerificationRepository phoneVerificationRepository;
	private final PhoneVerificationCodeStore phoneVerificationCodeStore;
	private final PhoneVerificationSender phoneVerificationSender;
	private final String hashSecret;
	private final Clock clock;
	private final IntSupplier verificationCodeSupplier;

	@Autowired
	public SendPhoneVerificationCodeService(
			final UserAccountRepository userAccountRepository,
			final PhoneVerificationRepository phoneVerificationRepository,
			final PhoneVerificationCodeStore phoneVerificationCodeStore,
			final PhoneVerificationSender phoneVerificationSender,
			@Value("${nearby.phone-verification.hash-secret}") final String hashSecret
	) {
		this(
				userAccountRepository,
				phoneVerificationRepository,
				phoneVerificationCodeStore,
				phoneVerificationSender,
				hashSecret,
				Clock.systemUTC(),
				() -> SECURE_RANDOM.nextInt(CODE_BOUND)
		);
	}

	SendPhoneVerificationCodeService(
			final UserAccountRepository userAccountRepository,
			final PhoneVerificationRepository phoneVerificationRepository,
			final PhoneVerificationCodeStore phoneVerificationCodeStore,
			final PhoneVerificationSender phoneVerificationSender,
			final String hashSecret,
			final Clock clock,
			final IntSupplier verificationCodeSupplier
	) {
		this.userAccountRepository = userAccountRepository;
		this.phoneVerificationRepository = phoneVerificationRepository;
		this.phoneVerificationCodeStore = phoneVerificationCodeStore;
		this.phoneVerificationSender = phoneVerificationSender;
		this.hashSecret = hashSecret;
		this.clock = clock;
		this.verificationCodeSupplier = verificationCodeSupplier;
	}

	@Override
	public SendPhoneVerificationCodeResult send(final SendPhoneVerificationCodeCommand command) {
		UserAccount userAccount = userAccountRepository.findById(command.userId())
				.orElseThrow(UserNotFoundException::new);
		String verificationCode = verificationCode();
		String verificationCodeHash = PhoneVerificationCodeHasher.hmacSha256(verificationCode, hashSecret);
		PhoneVerification phoneVerification = phoneVerificationRepository.save(new PhoneVerification(
				null,
				userAccount.id(),
				command.phoneNumber(),
				null,
				null,
				PhoneVerificationStatus.PENDING,
				LocalDateTime.now(clock).plusSeconds(EXPIRES_IN_SECONDS),
				null
		));

		try {
			phoneVerificationCodeStore.save(
					phoneVerification.id(),
					verificationCodeHash,
					Duration.ofSeconds(EXPIRES_IN_SECONDS)
			);
			phoneVerificationSender.send(command.phoneNumber(), verificationCode);
		} catch (RuntimeException exception) {
			markFailed(phoneVerification, exception);
			throw exception;
		}

		return new SendPhoneVerificationCodeResult(phoneVerification.id(), EXPIRES_IN_SECONDS);
	}

	private void markFailed(final PhoneVerification phoneVerification, final RuntimeException cause) {
		try {
			phoneVerificationRepository.save(new PhoneVerification(
					phoneVerification.id(),
					phoneVerification.userId(),
					phoneVerification.phoneNumber(),
					phoneVerification.carrier(),
					phoneVerification.verificationCodeHash(),
					PhoneVerificationStatus.FAILED,
					phoneVerification.expiresAt(),
					phoneVerification.verifiedAt()
			));
		} catch (RuntimeException cleanupException) {
			cause.addSuppressed(cleanupException);
		}
		try {
			phoneVerificationCodeStore.delete(phoneVerification.id());
		} catch (RuntimeException cleanupException) {
			cause.addSuppressed(cleanupException);
		}
	}

	private String verificationCode() {
		return "%06d".formatted(Math.floorMod(verificationCodeSupplier.getAsInt(), CODE_BOUND));
	}
}
