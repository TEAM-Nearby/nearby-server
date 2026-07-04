// 휴대폰 인증 번호를 검증하고 사용자 온보딩 상태를 갱신하는 유스케이스
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.PhoneVerification;
import com.sopt.nearby.user.domain.model.PhoneVerificationStatus;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.exception.PhoneVerificationCodeMismatchException;
import com.sopt.nearby.user.exception.PhoneVerificationExpiredException;
import com.sopt.nearby.user.exception.PhoneVerificationNotFoundException;
import com.sopt.nearby.user.exception.UserNotFoundException;
import com.sopt.nearby.user.port.in.ConfirmPhoneVerificationCodeUseCase;
import com.sopt.nearby.user.port.out.PhoneVerificationRepository;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmPhoneVerificationCodeService implements ConfirmPhoneVerificationCodeUseCase {

	private static final String VERIFICATION_CODE_PATTERN = "\\d{6}";

	private final UserAccountRepository userAccountRepository;
	private final PhoneVerificationRepository phoneVerificationRepository;
	private final String hashSecret;
	private final Clock clock;

	@Autowired
	public ConfirmPhoneVerificationCodeService(
			final UserAccountRepository userAccountRepository,
			final PhoneVerificationRepository phoneVerificationRepository,
			@Value("${nearby.phone-verification.hash-secret}") final String hashSecret
	) {
		this(userAccountRepository, phoneVerificationRepository, hashSecret, Clock.systemUTC());
	}

	ConfirmPhoneVerificationCodeService(
			final UserAccountRepository userAccountRepository,
			final PhoneVerificationRepository phoneVerificationRepository,
			final String hashSecret,
			final Clock clock
	) {
		this.userAccountRepository = userAccountRepository;
		this.phoneVerificationRepository = phoneVerificationRepository;
		this.hashSecret = hashSecret;
		this.clock = clock;
	}

	@Override
	@Transactional
	public ConfirmPhoneVerificationCodeResult confirm(final ConfirmPhoneVerificationCodeCommand command) {
		PhoneVerification phoneVerification = phoneVerificationRepository.findById(command.phoneVerificationId())
				.orElseThrow(PhoneVerificationNotFoundException::new);
		if (!phoneVerification.userId().equals(command.userId())) {
			throw new PhoneVerificationNotFoundException();
		}
		if (phoneVerification.status() != PhoneVerificationStatus.PENDING) {
			throw new PhoneVerificationCodeMismatchException();
		}

		LocalDateTime now = LocalDateTime.now(clock);
		if (phoneVerification.expiresAt().isBefore(now)) {
			throw new PhoneVerificationExpiredException();
		}
		if (!matches(phoneVerification.verificationCodeHash(), command.verificationCode())) {
			throw new PhoneVerificationCodeMismatchException();
		}

		phoneVerificationRepository.save(new PhoneVerification(
				phoneVerification.id(),
				phoneVerification.userId(),
				phoneVerification.phoneNumber(),
				phoneVerification.carrier(),
				phoneVerification.verificationCodeHash(),
				PhoneVerificationStatus.VERIFIED,
				phoneVerification.expiresAt(),
				now
		));

		UserAccount userAccount = userAccountRepository.findById(command.userId())
				.orElseThrow(UserNotFoundException::new);
		userAccountRepository.save(new UserAccount(
				userAccount.id(),
				userAccount.role(),
				userAccount.status(),
				phoneVerification.phoneNumber(),
				now,
				UserOnboardingStatus.PHONE_VERIFIED,
				userAccount.createdAt(),
				userAccount.deletedAt()
		));

		return new ConfirmPhoneVerificationCodeResult(true, UserOnboardingStatus.PHONE_VERIFIED);
	}

	private boolean matches(final String expectedHash, final String verificationCode) {
		if (verificationCode == null || !verificationCode.matches(VERIFICATION_CODE_PATTERN)) {
			return false;
		}

		return MessageDigest.isEqual(
				expectedHash.getBytes(StandardCharsets.UTF_8),
				PhoneVerificationCodeHasher.hmacSha256(verificationCode, hashSecret).getBytes(StandardCharsets.UTF_8)
		);
	}
}
