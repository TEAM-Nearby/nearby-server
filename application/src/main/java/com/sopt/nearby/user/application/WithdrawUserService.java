// 소셜 계정 연동을 해제하고 회원 개인정보를 정리하는 탈퇴 서비스
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.AppleRefreshToken;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.exception.AppleReauthenticationRequiredException;
import com.sopt.nearby.user.exception.SocialAccountUnlinkFailedException;
import com.sopt.nearby.user.exception.UserAccountNotFoundException;
import com.sopt.nearby.user.exception.UserAlreadyWithdrawnException;
import com.sopt.nearby.user.port.in.WithdrawUserUseCase;
import com.sopt.nearby.user.port.out.AppleOAuthClient;
import com.sopt.nearby.user.port.out.AppleRefreshTokenRepository;
import com.sopt.nearby.user.port.out.EmergencyContactRepository;
import com.sopt.nearby.user.port.out.KakaoAccountUnlinker;
import com.sopt.nearby.user.port.out.PhoneVerificationRepository;
import com.sopt.nearby.user.port.out.SocialAccountRepository;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WithdrawUserService implements WithdrawUserUseCase {

	private static final String KAKAO_PROVIDER = "KAKAO";
	private static final String APPLE_PROVIDER = "APPLE";

	private final UserAccountRepository userAccountRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final AppleRefreshTokenRepository appleRefreshTokenRepository;
	private final EmergencyContactRepository emergencyContactRepository;
	private final PhoneVerificationRepository phoneVerificationRepository;
	private final KakaoAccountUnlinker kakaoAccountUnlinker;
	private final AppleOAuthClient appleOAuthClient;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

	public WithdrawUserService(
			final UserAccountRepository userAccountRepository,
			final SocialAccountRepository socialAccountRepository,
			final AppleRefreshTokenRepository appleRefreshTokenRepository,
			final EmergencyContactRepository emergencyContactRepository,
			final PhoneVerificationRepository phoneVerificationRepository,
			final KakaoAccountUnlinker kakaoAccountUnlinker,
			final AppleOAuthClient appleOAuthClient,
			final ApplicationEventPublisher eventPublisher,
			final Clock clock
	) {
		this.userAccountRepository = userAccountRepository;
		this.socialAccountRepository = socialAccountRepository;
		this.appleRefreshTokenRepository = appleRefreshTokenRepository;
		this.emergencyContactRepository = emergencyContactRepository;
		this.phoneVerificationRepository = phoneVerificationRepository;
		this.kakaoAccountUnlinker = kakaoAccountUnlinker;
		this.appleOAuthClient = appleOAuthClient;
		this.eventPublisher = eventPublisher;
		this.clock = clock;
	}

	@Override
	@Transactional
	public WithdrawUserResult withdraw(final WithdrawUserCommand command) {
		UserAccount user = userAccountRepository.findByIdForUpdate(command.userId())
				.orElseThrow(UserAccountNotFoundException::new);
		if (user.status() == UserAccountStatus.WITHDRAWN) {
			throw new UserAlreadyWithdrawnException();
		}

		List<SocialAccount> socialAccounts = socialAccountRepository.findAllByUserId(user.id());
		if (socialAccounts.isEmpty()) {
			throw new UserAccountNotFoundException();
		}
		validateUnlinkPrerequisites(user.id(), socialAccounts);
		socialAccounts.forEach(this::unlink);
		cleanPersonalData(user);
		return new WithdrawUserResult(true);
	}

	private void validateUnlinkPrerequisites(final Long userId, final List<SocialAccount> socialAccounts) {
		boolean hasAppleAccount = socialAccounts.stream()
				.anyMatch(account -> APPLE_PROVIDER.equals(account.provider()));
		if (hasAppleAccount && appleRefreshTokenRepository.findByUserId(userId).isEmpty()) {
			throw new AppleReauthenticationRequiredException();
		}
	}

	private void unlink(final SocialAccount socialAccount) {
		switch (socialAccount.provider()) {
			case KAKAO_PROVIDER -> kakaoAccountUnlinker.unlink(socialAccount.providerUserId());
			case APPLE_PROVIDER -> {
				AppleRefreshToken token = appleRefreshTokenRepository.findByUserId(socialAccount.userId())
						.orElseThrow(AppleReauthenticationRequiredException::new);
				appleOAuthClient.revoke(token.refreshToken());
			}
			default -> throw new SocialAccountUnlinkFailedException();
		}
	}

	private void cleanPersonalData(final UserAccount user) {
		LocalDateTime now = LocalDateTime.now(clock);
		emergencyContactRepository.deleteByUserId(user.id());
		phoneVerificationRepository.deleteByUserId(user.id());
		socialAccountRepository.deleteByUserId(user.id());
		appleRefreshTokenRepository.deleteByUserId(user.id());
		userAccountRepository.save(new UserAccount(
				user.id(),
				user.role(),
				UserAccountStatus.WITHDRAWN,
				null,
				null,
				user.onboardingStatus(),
				user.createdAt(),
				now
		));
		eventPublisher.publishEvent(new com.sopt.nearby.user.port.in.UserWithdrawnEvent(user.id()));
	}
}
