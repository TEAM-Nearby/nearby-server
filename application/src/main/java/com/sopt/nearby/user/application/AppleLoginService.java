// 애플 ID 토큰을 검증하고 회원 토큰을 발급하는 유스케이스
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.AppleRefreshToken;
import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.exception.AppleLoginFailedException;
import com.sopt.nearby.user.exception.SocialAccountAlreadyExistsException;
import com.sopt.nearby.user.port.in.AppleLoginUseCase;
import com.sopt.nearby.user.port.out.AppleIdTokenVerifier;
import com.sopt.nearby.user.port.out.AppleOAuthClient;
import com.sopt.nearby.user.port.out.AppleRefreshTokenRepository;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import com.sopt.nearby.user.port.out.SocialAccountRepository;
import com.sopt.nearby.user.port.out.TokenIssuer;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AppleLoginService implements AppleLoginUseCase {

	private static final String APPLE_PROVIDER = "APPLE";
	private static final String TOKEN_TYPE = "Bearer";

	private final AppleIdTokenVerifier appleIdTokenVerifier;
	private final AppleOAuthClient appleOAuthClient;
	private final AppleRefreshTokenRepository appleRefreshTokenRepository;
	private final TokenIssuer tokenIssuer;
	private final UserAccountRepository userAccountRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final Clock clock;
	private final TransactionOperations createUserTransaction;

	@Autowired
	public AppleLoginService(
			final AppleIdTokenVerifier appleIdTokenVerifier,
			final AppleOAuthClient appleOAuthClient,
			final AppleRefreshTokenRepository appleRefreshTokenRepository,
			final TokenIssuer tokenIssuer,
			final UserAccountRepository userAccountRepository,
			final SocialAccountRepository socialAccountRepository,
			final RefreshTokenRepository refreshTokenRepository,
			final PlatformTransactionManager transactionManager,
			final Clock clock
	) {
		this(
				appleIdTokenVerifier,
				appleOAuthClient,
				appleRefreshTokenRepository,
				tokenIssuer,
				userAccountRepository,
				socialAccountRepository,
				refreshTokenRepository,
				clock,
				requiresNewTransaction(transactionManager)
		);
	}

	AppleLoginService(
			final AppleIdTokenVerifier appleIdTokenVerifier,
			final AppleOAuthClient appleOAuthClient,
			final AppleRefreshTokenRepository appleRefreshTokenRepository,
			final TokenIssuer tokenIssuer,
			final UserAccountRepository userAccountRepository,
			final SocialAccountRepository socialAccountRepository,
			final RefreshTokenRepository refreshTokenRepository,
			final Clock clock
	) {
		this(
				appleIdTokenVerifier,
				appleOAuthClient,
				appleRefreshTokenRepository,
				tokenIssuer,
				userAccountRepository,
				socialAccountRepository,
				refreshTokenRepository,
				clock,
				withoutTransaction()
		);
	}

	AppleLoginService(
			final AppleIdTokenVerifier appleIdTokenVerifier,
			final AppleOAuthClient appleOAuthClient,
			final AppleRefreshTokenRepository appleRefreshTokenRepository,
			final TokenIssuer tokenIssuer,
			final UserAccountRepository userAccountRepository,
			final SocialAccountRepository socialAccountRepository,
			final RefreshTokenRepository refreshTokenRepository,
			final Clock clock,
			final TransactionOperations createUserTransaction
	) {
		this.appleIdTokenVerifier = appleIdTokenVerifier;
		this.appleOAuthClient = appleOAuthClient;
		this.appleRefreshTokenRepository = appleRefreshTokenRepository;
		this.tokenIssuer = tokenIssuer;
		this.userAccountRepository = userAccountRepository;
		this.socialAccountRepository = socialAccountRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.clock = clock;
		this.createUserTransaction = createUserTransaction;
	}

	@Override
	@Transactional
	public AppleLoginResult login(final AppleLoginCommand command) {
		VerifiedUser appleUser = appleIdTokenVerifier.verify(command.idToken(), command.nonce());
		String appleRefreshToken = appleOAuthClient.exchangeAuthorizationCode(command.authorizationCode());
		UserAccount userAccount = findOrCreateUser(appleUser.providerUserId());
		appleRefreshTokenRepository.save(new AppleRefreshToken(
				userAccount.id(),
				appleRefreshToken,
				LocalDateTime.now(clock)
		));
		IssuedTokens tokens = tokenIssuer.issue(new TokenIssueRequest(
				userAccount.id(),
				userAccount.role(),
				userAccount.onboardingStatus()
		));

		refreshTokenRepository.save(new RefreshToken(
				null,
				userAccount.id(),
				tokens.refreshTokenHash(),
				LocalDateTime.now(clock).plusSeconds(tokens.refreshTokenExpiresIn()),
				null
		));

		return new AppleLoginResult(
				tokens.accessToken(),
				tokens.refreshToken(),
				TOKEN_TYPE,
				tokens.accessTokenExpiresIn(),
				tokens.refreshTokenExpiresIn(),
				userAccount.id(),
				userAccount.onboardingStatus()
		);
	}

	private UserAccount findOrCreateUser(final String providerUserId) {
		return socialAccountRepository.findByProviderAndProviderUserId(APPLE_PROVIDER, providerUserId)
				.map(this::findUser)
				.orElseGet(() -> createUser(providerUserId));
	}

	private UserAccount findUser(final SocialAccount socialAccount) {
		UserAccount userAccount = userAccountRepository.findById(socialAccount.userId())
				.orElseThrow(AppleLoginFailedException::new);
		if (userAccount.status() != UserAccountStatus.ACTIVE) {
			throw new AppleLoginFailedException();
		}
		return userAccount;
	}

	private UserAccount createUser(final String providerUserId) {
		try {
			return createUserTransaction.execute(status -> createNewUser(providerUserId));
		} catch (SocialAccountAlreadyExistsException exception) {
			return findExistingUser(providerUserId);
		}
	}

	private UserAccount findExistingUser(final String providerUserId) {
		return socialAccountRepository.findByProviderAndProviderUserId(APPLE_PROVIDER, providerUserId)
				.map(this::findUser)
				.orElseThrow(AppleLoginFailedException::new);
	}

	private UserAccount createNewUser(final String providerUserId) {
		LocalDateTime now = LocalDateTime.now(clock);
		UserAccount userAccount = userAccountRepository.save(new UserAccount(
				null,
				UserRole.USER,
				UserAccountStatus.ACTIVE,
				null,
				null,
				UserOnboardingStatus.STARTED,
				now,
				null
		));
		socialAccountRepository.save(new SocialAccount(null, userAccount.id(), APPLE_PROVIDER, providerUserId));
		return userAccount;
	}

	private static TransactionOperations requiresNewTransaction(final PlatformTransactionManager transactionManager) {
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return new TransactionTemplate(transactionManager, definition);
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
