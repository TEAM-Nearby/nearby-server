// 카카오 ID 토큰을 검증하고 회원 토큰을 발급하는 유스케이스
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.exception.KakaoLoginFailedException;
import com.sopt.nearby.user.port.in.KakaoLoginUseCase;
import com.sopt.nearby.user.port.out.KakaoIdTokenVerifier;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import com.sopt.nearby.user.port.out.SocialAccountRepository;
import com.sopt.nearby.user.port.out.TokenIssuer;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KakaoLoginService implements KakaoLoginUseCase {

	private static final String KAKAO_PROVIDER = "KAKAO";
	private static final String TOKEN_TYPE = "Bearer";

	private final KakaoIdTokenVerifier kakaoIdTokenVerifier;
	private final TokenIssuer tokenIssuer;
	private final UserAccountRepository userAccountRepository;
	private final SocialAccountRepository socialAccountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final Clock clock;

	@Autowired
	public KakaoLoginService(
			final KakaoIdTokenVerifier kakaoIdTokenVerifier,
			final TokenIssuer tokenIssuer,
			final UserAccountRepository userAccountRepository,
			final SocialAccountRepository socialAccountRepository,
			final RefreshTokenRepository refreshTokenRepository
	) {
		this(
				kakaoIdTokenVerifier,
				tokenIssuer,
				userAccountRepository,
				socialAccountRepository,
				refreshTokenRepository,
				Clock.systemUTC()
		);
	}

	KakaoLoginService(
			final KakaoIdTokenVerifier kakaoIdTokenVerifier,
			final TokenIssuer tokenIssuer,
			final UserAccountRepository userAccountRepository,
			final SocialAccountRepository socialAccountRepository,
			final RefreshTokenRepository refreshTokenRepository,
			final Clock clock
	) {
		this.kakaoIdTokenVerifier = kakaoIdTokenVerifier;
		this.tokenIssuer = tokenIssuer;
		this.userAccountRepository = userAccountRepository;
		this.socialAccountRepository = socialAccountRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.clock = clock;
	}

	@Override
	@Transactional
	public KakaoLoginResult login(final KakaoLoginCommand command) {
		VerifiedKakaoUser kakaoUser = kakaoIdTokenVerifier.verify(command.idToken(), command.nonce());
		UserAccount userAccount = findOrCreateUser(kakaoUser.providerUserId());
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

		return new KakaoLoginResult(
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
		return socialAccountRepository.findByProviderAndProviderUserId(KAKAO_PROVIDER, providerUserId)
				.map(this::findUser)
				.orElseGet(() -> createUser(providerUserId));
	}

	private UserAccount findUser(final SocialAccount socialAccount) {
		return userAccountRepository.findById(socialAccount.userId())
				.orElseThrow(KakaoLoginFailedException::new);
	}

	private UserAccount createUser(final String providerUserId) {
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
		socialAccountRepository.save(new SocialAccount(null, userAccount.id(), KAKAO_PROVIDER, providerUserId));
		return userAccount;
	}
}
