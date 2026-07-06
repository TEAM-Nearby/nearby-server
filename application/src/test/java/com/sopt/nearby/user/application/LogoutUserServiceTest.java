// 로그아웃 유스케이스의 리프레시 토큰 검증과 만료 처리를 검증하는 테스트
package com.sopt.nearby.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.exception.InvalidLogoutRequestException;
import com.sopt.nearby.user.exception.InvalidRefreshTokenException;
import com.sopt.nearby.user.exception.RefreshTokenAlreadyRevokedException;
import com.sopt.nearby.user.port.out.RefreshTokenHasher;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LogoutUserServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-07T12:00:00Z"), ZoneId.of("UTC"));
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 7, 12, 0);

	@Test
	void revokesRefreshTokenWhenLogoutRequestIsValid() {
		FakeRefreshTokenRepository repository = new FakeRefreshTokenRepository();
		repository.put(new RefreshToken(1L, 7L, "hash-refresh-token", NOW.plusDays(1), null));
		LogoutUserService service = service(repository);

		LogoutUserResult result = service.logout(new LogoutUserCommand(7L, "refresh-token"));

		assertTrue(result.loggedOut());
		assertEquals(NOW, repository.findByTokenHash("hash-refresh-token").orElseThrow().revokedAt());
		assertEquals(1, repository.revokeCount);
	}

	@Test
	void rejectsBlankRefreshToken() {
		LogoutUserService service = service(new FakeRefreshTokenRepository());

		assertThrows(InvalidLogoutRequestException.class, () -> service.logout(new LogoutUserCommand(7L, "")));
	}

	@Test
	void rejectsMissingRefreshToken() {
		LogoutUserService service = service(new FakeRefreshTokenRepository());

		assertThrows(InvalidLogoutRequestException.class, () -> service.logout(new LogoutUserCommand(7L, null)));
	}

	@Test
	void rejectsUnknownRefreshToken() {
		LogoutUserService service = service(new FakeRefreshTokenRepository());

		assertThrows(InvalidRefreshTokenException.class, () -> service.logout(new LogoutUserCommand(7L, "missing")));
	}

	@Test
	void rejectsRefreshTokenOwnedByAnotherUser() {
		FakeRefreshTokenRepository repository = new FakeRefreshTokenRepository();
		repository.put(new RefreshToken(1L, 99L, "hash-refresh-token", NOW.plusDays(1), null));
		LogoutUserService service = service(repository);

		assertThrows(InvalidRefreshTokenException.class, () -> service.logout(new LogoutUserCommand(7L, "refresh-token")));
	}

	@Test
	void rejectsExpiredRefreshToken() {
		FakeRefreshTokenRepository repository = new FakeRefreshTokenRepository();
		repository.put(new RefreshToken(1L, 7L, "hash-refresh-token", NOW.minusSeconds(1), null));
		LogoutUserService service = service(repository);

		assertThrows(InvalidRefreshTokenException.class, () -> service.logout(new LogoutUserCommand(7L, "refresh-token")));
	}

	@Test
	void rejectsAlreadyRevokedRefreshToken() {
		FakeRefreshTokenRepository repository = new FakeRefreshTokenRepository();
		repository.put(new RefreshToken(1L, 7L, "hash-refresh-token", NOW.plusDays(1), NOW.minusMinutes(1)));
		LogoutUserService service = service(repository);

		assertThrows(RefreshTokenAlreadyRevokedException.class, () -> service.logout(new LogoutUserCommand(7L, "refresh-token")));
	}

	@Test
	void rejectsWhenConditionalRevokeLosesRace() {
		FakeRefreshTokenRepository repository = new FakeRefreshTokenRepository();
		repository.put(new RefreshToken(1L, 7L, "hash-refresh-token", NOW.plusDays(1), null));
		repository.failRevoke = true;
		LogoutUserService service = service(repository);

		assertThrows(RefreshTokenAlreadyRevokedException.class, () -> service.logout(new LogoutUserCommand(7L, "refresh-token")));
	}

	private LogoutUserService service(final FakeRefreshTokenRepository repository) {
		return new LogoutUserService(repository, new FakeRefreshTokenHasher(), CLOCK);
	}

	private static final class FakeRefreshTokenHasher implements RefreshTokenHasher {

		@Override
		public String hash(final String refreshToken) {
			return "hash-" + refreshToken;
		}
	}

	private static final class FakeRefreshTokenRepository implements RefreshTokenRepository {

		private final Map<Long, RefreshToken> tokens = new HashMap<>();
		private int revokeCount = 0;
		private boolean failRevoke = false;

		@Override
		public RefreshToken save(final RefreshToken model) {
			tokens.put(model.id(), model);
			return model;
		}

		@Override
		public Optional<RefreshToken> findById(final Long id) {
			return Optional.ofNullable(tokens.get(id));
		}

		@Override
		public Optional<RefreshToken> findByTokenHash(final String tokenHash) {
			return tokens.values()
					.stream()
					.filter(token -> token.tokenHash().equals(tokenHash))
					.findFirst();
		}

		@Override
		public boolean revokeByTokenHashIfActive(
				final String tokenHash,
				final Long userId,
				final LocalDateTime revokedAt
		) {
			revokeCount++;
			if (failRevoke) {
				return false;
			}
			Optional<RefreshToken> found = findByTokenHash(tokenHash)
					.filter(token -> token.userId().equals(userId))
					.filter(token -> token.revokedAt() == null);
			found.ifPresent(token -> tokens.put(token.id(), new RefreshToken(
					token.id(),
					token.userId(),
					token.tokenHash(),
					token.expiresAt(),
					revokedAt
			)));
			return found.isPresent();
		}

		private void put(final RefreshToken refreshToken) {
			tokens.put(refreshToken.id(), refreshToken);
		}
	}
}
