// 휴대폰 인증 코드를 생성, 저장하고 문자 발송 포트로 전달하는 유스케이스
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.PhoneVerification;
import com.sopt.nearby.user.domain.model.PhoneVerificationStatus;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.exception.UserNotFoundException;
import com.sopt.nearby.user.port.in.SendPhoneVerificationCodeUseCase;
import com.sopt.nearby.user.port.out.PhoneVerificationRepository;
import com.sopt.nearby.user.port.out.PhoneVerificationSender;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.function.IntSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SendPhoneVerificationCodeService implements SendPhoneVerificationCodeUseCase {

	private static final int EXPIRES_IN_SECONDS = 180;
	private static final int CODE_BOUND = 1_000_000;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserAccountRepository userAccountRepository;
	private final PhoneVerificationRepository phoneVerificationRepository;
	private final PhoneVerificationSender phoneVerificationSender;
	private final Clock clock;
	private final IntSupplier verificationCodeSupplier;

	@Autowired
	public SendPhoneVerificationCodeService(
			final UserAccountRepository userAccountRepository,
			final PhoneVerificationRepository phoneVerificationRepository,
			final PhoneVerificationSender phoneVerificationSender
	) {
		this(
				userAccountRepository,
				phoneVerificationRepository,
				phoneVerificationSender,
				Clock.systemUTC(),
				() -> SECURE_RANDOM.nextInt(CODE_BOUND)
		);
	}

	SendPhoneVerificationCodeService(
			final UserAccountRepository userAccountRepository,
			final PhoneVerificationRepository phoneVerificationRepository,
			final PhoneVerificationSender phoneVerificationSender,
			final Clock clock,
			final IntSupplier verificationCodeSupplier
	) {
		this.userAccountRepository = userAccountRepository;
		this.phoneVerificationRepository = phoneVerificationRepository;
		this.phoneVerificationSender = phoneVerificationSender;
		this.clock = clock;
		this.verificationCodeSupplier = verificationCodeSupplier;
	}

	@Override
	@Transactional
	public SendPhoneVerificationCodeResult send(final SendPhoneVerificationCodeCommand command) {
		UserAccount userAccount = userAccountRepository.findById(command.userId())
				.orElseThrow(UserNotFoundException::new);
		String verificationCode = verificationCode();
		PhoneVerification phoneVerification = phoneVerificationRepository.save(new PhoneVerification(
				null,
				userAccount.id(),
				command.phoneNumber(),
				null,
				PhoneVerificationCodeHasher.sha256(verificationCode),
				PhoneVerificationStatus.PENDING,
				LocalDateTime.now(clock).plusSeconds(EXPIRES_IN_SECONDS),
				null
		));

		phoneVerificationSender.send(command.phoneNumber(), verificationCode);

		return new SendPhoneVerificationCodeResult(phoneVerification.id(), EXPIRES_IN_SECONDS);
	}

	private String verificationCode() {
		return "%06d".formatted(Math.floorMod(verificationCodeSupplier.getAsInt(), CODE_BOUND));
	}
}
