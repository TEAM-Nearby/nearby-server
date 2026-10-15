// 소셜 계정 연동을 해제하고 회원 개인정보를 정리하는 탈퇴 서비스
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.AppleRefreshToken;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.WithdrawalProgress;
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
import com.sopt.nearby.user.port.out.WithdrawalProgressRepository;
import com.sopt.nearby.user.port.out.UserWithdrawnEventPublisher;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

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
	private final WithdrawalProgressRepository withdrawalProgressRepository;
	private final UserWithdrawnEventPublisher eventPublisher;
	private final Clock clock;
	private final TransactionOperations transaction;

	@Autowired
	public WithdrawUserService(
			final UserAccountRepository userAccountRepository,
			final SocialAccountRepository socialAccountRepository,
			final AppleRefreshTokenRepository appleRefreshTokenRepository,
			final EmergencyContactRepository emergencyContactRepository,
			final PhoneVerificationRepository phoneVerificationRepository,
			final KakaoAccountUnlinker kakaoAccountUnlinker,
			final AppleOAuthClient appleOAuthClient,
			final WithdrawalProgressRepository withdrawalProgressRepository,
			final UserWithdrawnEventPublisher eventPublisher,
			final PlatformTransactionManager transactionManager,
			final Clock clock
	) {
		this(userAccountRepository, socialAccountRepository, appleRefreshTokenRepository,
				emergencyContactRepository, phoneVerificationRepository, kakaoAccountUnlinker,
				appleOAuthClient, withdrawalProgressRepository, eventPublisher,
				requiresNewTransaction(transactionManager), clock);
	}

	WithdrawUserService(
			final UserAccountRepository userAccountRepository,
			final SocialAccountRepository socialAccountRepository,
			final AppleRefreshTokenRepository appleRefreshTokenRepository,
			final EmergencyContactRepository emergencyContactRepository,
			final PhoneVerificationRepository phoneVerificationRepository,
			final KakaoAccountUnlinker kakaoAccountUnlinker,
			final AppleOAuthClient appleOAuthClient,
			final WithdrawalProgressRepository withdrawalProgressRepository,
			final UserWithdrawnEventPublisher eventPublisher,
			final Clock clock
	) {
		this(userAccountRepository, socialAccountRepository, appleRefreshTokenRepository,
				emergencyContactRepository, phoneVerificationRepository, kakaoAccountUnlinker,
				appleOAuthClient, withdrawalProgressRepository, eventPublisher, withoutTransaction(), clock);
	}

	WithdrawUserService(
			final UserAccountRepository userAccountRepository,
			final SocialAccountRepository socialAccountRepository,
			final AppleRefreshTokenRepository appleRefreshTokenRepository,
			final EmergencyContactRepository emergencyContactRepository,
			final PhoneVerificationRepository phoneVerificationRepository,
			final KakaoAccountUnlinker kakaoAccountUnlinker,
			final AppleOAuthClient appleOAuthClient,
			final WithdrawalProgressRepository withdrawalProgressRepository,
			final UserWithdrawnEventPublisher eventPublisher,
			final TransactionOperations transaction,
			final Clock clock
	) {
		this.userAccountRepository = userAccountRepository;
		this.socialAccountRepository = socialAccountRepository;
		this.appleRefreshTokenRepository = appleRefreshTokenRepository;
		this.emergencyContactRepository = emergencyContactRepository;
		this.phoneVerificationRepository = phoneVerificationRepository;
		this.kakaoAccountUnlinker = kakaoAccountUnlinker;
		this.appleOAuthClient = appleOAuthClient;
		this.withdrawalProgressRepository = withdrawalProgressRepository;
		this.eventPublisher = eventPublisher;
		this.transaction = transaction;
		this.clock = clock;
	}

	@Override
	public WithdrawUserResult withdraw(final WithdrawUserCommand command) {
		List<SocialAccount> socialAccounts = transaction.execute(status -> prepare(command.userId()));
		for (SocialAccount account : socialAccounts) {
			if (!transaction.execute(status -> beginUnlink(account))) {
				continue;
			}
			try {
				unlink(account);
			} catch (RuntimeException exception) {
				transaction.execute(status -> saveProgress(account, WithdrawalProgress.State.UNKNOWN));
				throw exception;
			}
			transaction.execute(status -> saveProgress(account, WithdrawalProgress.State.SUCCEEDED));
		}
		transaction.execute(status -> {
			finish(command.userId());
			return null;
		});
		return new WithdrawUserResult(true);
	}

	private List<SocialAccount> prepare(final Long userId) {
		UserAccount user = userAccountRepository.findByIdForUpdate(userId)
				.orElseThrow(UserAccountNotFoundException::new);
		if (user.status() == UserAccountStatus.WITHDRAWN) {
			throw new UserAlreadyWithdrawnException();
		}
		List<SocialAccount> socialAccounts = socialAccountRepository.findAllByUserId(user.id());
		if (socialAccounts.isEmpty()) {
			throw new UserAccountNotFoundException();
		}
		if (user.status() != UserAccountStatus.WITHDRAWING) {
			validateUnlinkPrerequisites(user.id(), socialAccounts);
			userAccountRepository.save(withStatus(user, UserAccountStatus.WITHDRAWING));
			for (SocialAccount account : socialAccounts) {
				withdrawalProgressRepository.save(new WithdrawalProgress(user.id(), account.provider(),
						WithdrawalProgress.State.PENDING));
			}
		}
		return socialAccounts;
	}

	private boolean beginUnlink(final SocialAccount account) {
		userAccountRepository.findByIdForUpdate(account.userId()).orElseThrow(UserAccountNotFoundException::new);
		WithdrawalProgress progress = withdrawalProgressRepository
				.findByUserIdAndProvider(account.userId(), account.provider())
				.orElseThrow(SocialAccountUnlinkFailedException::new);
		if (progress.state() == WithdrawalProgress.State.SUCCEEDED) {
			return false;
		}
		if (progress.state() != WithdrawalProgress.State.PENDING) {
			throw new SocialAccountUnlinkFailedException();
		}
		saveProgress(account, WithdrawalProgress.State.IN_FLIGHT);
		return true;
	}

	private Void saveProgress(final SocialAccount account, final WithdrawalProgress.State state) {
		withdrawalProgressRepository.save(new WithdrawalProgress(account.userId(), account.provider(), state));
		return null;
	}

	private void finish(final Long userId) {
		UserAccount user = userAccountRepository.findByIdForUpdate(userId)
				.orElseThrow(UserAccountNotFoundException::new);
		List<WithdrawalProgress> progress = withdrawalProgressRepository.findAllByUserId(userId);
		if (user.status() != UserAccountStatus.WITHDRAWING || progress.isEmpty()
				|| progress.stream().anyMatch(item -> item.state() != WithdrawalProgress.State.SUCCEEDED)) {
			throw new SocialAccountUnlinkFailedException();
		}
		cleanPersonalData(user);
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
		eventPublisher.publish(new com.sopt.nearby.user.port.in.UserWithdrawnEvent(user.id()));
	}

	private UserAccount withStatus(final UserAccount user, final UserAccountStatus status) {
		return new UserAccount(user.id(), user.role(), status, user.phoneNumber(), user.phoneVerifiedAt(),
				user.onboardingStatus(), user.createdAt(), user.deletedAt());
	}

	private static TransactionOperations requiresNewTransaction(final PlatformTransactionManager manager) {
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return new TransactionTemplate(manager, definition);
	}

	private static TransactionOperations withoutTransaction() {
		return new TransactionOperations() {
			@Override
			public <T> T execute(final TransactionCallback<T> action) {
				return action.doInTransaction(null);
			}
		};
	}
}
