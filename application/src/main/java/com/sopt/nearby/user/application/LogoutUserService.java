// 리프레시 토큰을 만료 처리해 사용자를 로그아웃시키는 유스케이스
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.exception.InvalidLogoutRequestException;
import com.sopt.nearby.user.exception.InvalidRefreshTokenException;
import com.sopt.nearby.user.exception.RefreshTokenAlreadyRevokedException;
import com.sopt.nearby.user.port.in.LogoutUserUseCase;
import com.sopt.nearby.user.port.out.RefreshTokenHasher;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUserService implements LogoutUserUseCase {

	private final RefreshTokenRepository refreshTokenRepository;
	private final RefreshTokenHasher refreshTokenHasher;
	private final Clock clock;

	@Autowired
	public LogoutUserService(
			final RefreshTokenRepository refreshTokenRepository,
			final RefreshTokenHasher refreshTokenHasher,
			final Clock clock
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.refreshTokenHasher = refreshTokenHasher;
		this.clock = clock;
	}

	@Override
	@Transactional
	public LogoutUserResult logout(final LogoutUserCommand command) {
		validate(command);

		String tokenHash = refreshTokenHasher.hash(command.refreshToken());
		RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(InvalidRefreshTokenException::new);

		if (!refreshToken.userId().equals(command.userId())) {
			throw new InvalidRefreshTokenException();
		}
		if (refreshToken.revokedAt() != null) {
			throw new RefreshTokenAlreadyRevokedException();
		}

		LocalDateTime now = LocalDateTime.now(clock);
		if (refreshToken.expiresAt().isBefore(now)) {
			throw new InvalidRefreshTokenException();
		}

		boolean revoked = refreshTokenRepository.revokeByTokenHashIfActive(
				tokenHash,
				command.userId(),
				now
		);
		if (!revoked) {
			throw new RefreshTokenAlreadyRevokedException();
		}

		return new LogoutUserResult(true);
	}

	private void validate(final LogoutUserCommand command) {
		if (command == null || command.userId() == null || command.refreshToken() == null
				|| command.refreshToken().isBlank()) {
			throw new InvalidLogoutRequestException();
		}
	}
}
