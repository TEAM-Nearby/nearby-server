// 리프레시 토큰을 검증하고 새 토큰 쌍으로 회전하는 유스케이스 서비스
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.exception.InvalidRefreshTokenException;
import com.sopt.nearby.user.exception.InvalidTokenRefreshRequestException;
import com.sopt.nearby.user.exception.RefreshTokenAlreadyRevokedException;
import com.sopt.nearby.user.port.in.RefreshTokenUseCase;
import com.sopt.nearby.user.port.out.RefreshTokenHasher;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import com.sopt.nearby.user.port.out.TokenIssuer;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService implements RefreshTokenUseCase {

	private static final String TOKEN_TYPE = "Bearer";

	private final RefreshTokenHasher refreshTokenHasher;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserAccountRepository userAccountRepository;
	private final TokenIssuer tokenIssuer;
	private final Clock clock;

	@Autowired
	public RefreshTokenService(
			final RefreshTokenHasher refreshTokenHasher,
			final RefreshTokenRepository refreshTokenRepository,
			final UserAccountRepository userAccountRepository,
			final TokenIssuer tokenIssuer
	) {
		this(refreshTokenHasher, refreshTokenRepository, userAccountRepository, tokenIssuer, Clock.systemUTC());
	}

	RefreshTokenService(
			final RefreshTokenHasher refreshTokenHasher,
			final RefreshTokenRepository refreshTokenRepository,
			final UserAccountRepository userAccountRepository,
			final TokenIssuer tokenIssuer,
			final Clock clock
	) {
		this.refreshTokenHasher = refreshTokenHasher;
		this.refreshTokenRepository = refreshTokenRepository;
		this.userAccountRepository = userAccountRepository;
		this.tokenIssuer = tokenIssuer;
		this.clock = clock;
	}

	@Override
	@Transactional
	public RefreshTokenResult refresh(final RefreshTokenCommand command) {
		validate(command);
		String tokenHash = refreshTokenHasher.hash(command.refreshToken());
		RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(InvalidRefreshTokenException::new);
		LocalDateTime now = LocalDateTime.now(clock);
		if (refreshToken.revokedAt() != null) {
			throw new RefreshTokenAlreadyRevokedException();
		}
		if (!refreshToken.expiresAt().isAfter(now)) {
			throw new InvalidRefreshTokenException();
		}
		UserAccount userAccount = userAccountRepository.findById(refreshToken.userId())
				.orElseThrow(InvalidRefreshTokenException::new);
		IssuedTokens tokens = tokenIssuer.issue(new TokenIssueRequest(
				userAccount.id(), userAccount.role(), userAccount.onboardingStatus()
		));
		if (!refreshTokenRepository.revokeByTokenHashIfActive(tokenHash, userAccount.id(), now)) {
			throw new RefreshTokenAlreadyRevokedException();
		}
		refreshTokenRepository.save(new RefreshToken(
				null,
				userAccount.id(),
				tokens.refreshTokenHash(),
				now.plusSeconds(tokens.refreshTokenExpiresIn()),
				null
		));
		return new RefreshTokenResult(
				tokens.accessToken(),
				tokens.refreshToken(),
				TOKEN_TYPE,
				tokens.accessTokenExpiresIn(),
				tokens.refreshTokenExpiresIn()
		);
	}

	private void validate(final RefreshTokenCommand command) {
		if (command == null || command.refreshToken() == null || command.refreshToken().isBlank()) {
			throw new InvalidTokenRefreshRequestException();
		}
	}
}
