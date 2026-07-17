// 리프레시 토큰 재발급 유스케이스의 토큰 회전과 검증을 확인하는 테스트
package com.sopt.nearby.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.exception.InvalidRefreshTokenException;
import com.sopt.nearby.user.exception.InvalidTokenRefreshRequestException;
import com.sopt.nearby.user.exception.RefreshTokenAlreadyRevokedException;
import com.sopt.nearby.user.port.out.RefreshTokenHasher;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import com.sopt.nearby.user.port.out.TokenIssuer;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-07T03:00:00Z"), ZoneId.of("Asia/Seoul"));
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 7, 12, 0);

	@Test
	void rotatesTokensWhenRefreshTokenIsActive() {
		FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
		refreshTokens.put(new RefreshToken(1L, 7L, "hash-old", NOW.plusDays(1), null));
		RefreshTokenService service = service(refreshTokens, userAccounts(7L));

		RefreshTokenResult result = service.refresh(new RefreshTokenCommand("old"));

		assertEquals("access-new", result.accessToken());
		assertEquals("refresh-new", result.refreshToken());
		assertEquals(NOW, refreshTokens.findByTokenHash("hash-old").orElseThrow().revokedAt());
		RefreshToken saved = refreshTokens.findByTokenHash("hash-refresh-new").orElseThrow();
		assertEquals(7L, saved.userId());
		assertEquals(NOW.plusDays(14), saved.expiresAt());
	}

	@Test
	void rejectsBlankRefreshToken() {
		assertThrows(InvalidTokenRefreshRequestException.class,
				() -> service(new FakeRefreshTokenRepository(), userAccounts(7L)).refresh(new RefreshTokenCommand("")));
	}

	@Test
	void rejectsUnknownRefreshToken() {
		assertThrows(InvalidRefreshTokenException.class,
				() -> service(new FakeRefreshTokenRepository(), userAccounts(7L)).refresh(new RefreshTokenCommand("missing")));
	}

	@Test
	void rejectsExpiredRefreshToken() {
		FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
		refreshTokens.put(new RefreshToken(1L, 7L, "hash-old", NOW.minusSeconds(1), null));

		assertThrows(InvalidRefreshTokenException.class,
				() -> service(refreshTokens, userAccounts(7L)).refresh(new RefreshTokenCommand("old")));
	}

	@Test
	void rejectsAlreadyRevokedRefreshToken() {
		FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
		refreshTokens.put(new RefreshToken(1L, 7L, "hash-old", NOW.plusDays(1), NOW.minusMinutes(1)));

		assertThrows(RefreshTokenAlreadyRevokedException.class,
				() -> service(refreshTokens, userAccounts(7L)).refresh(new RefreshTokenCommand("old")));
	}

	@Test
	void rejectsWhenConditionalRevokeLosesRace() {
		FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
		refreshTokens.put(new RefreshToken(1L, 7L, "hash-old", NOW.plusDays(1), null));
		refreshTokens.failRevoke = true;

		assertThrows(RefreshTokenAlreadyRevokedException.class,
				() -> service(refreshTokens, userAccounts(7L)).refresh(new RefreshTokenCommand("old")));
	}

	@Test
	void rejectsRefreshTokenWithoutUserAccount() {
		FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
		refreshTokens.put(new RefreshToken(1L, 7L, "hash-old", NOW.plusDays(1), null));

		assertThrows(InvalidRefreshTokenException.class,
				() -> service(refreshTokens, userAccounts()).refresh(new RefreshTokenCommand("old")));
	}

	private RefreshTokenService service(
			final FakeRefreshTokenRepository refreshTokens,
			final FakeUserAccountRepository userAccounts
	) {
		return new RefreshTokenService(
				new FakeRefreshTokenHasher(),
				refreshTokens,
				userAccounts,
				new FakeTokenIssuer(),
				CLOCK
		);
	}

	private FakeUserAccountRepository userAccounts(final Long... userIds) {
		FakeUserAccountRepository repository = new FakeUserAccountRepository();
		for (Long userId : userIds) {
			repository.save(new UserAccount(
					userId, UserRole.USER, UserAccountStatus.ACTIVE, null, null,
					UserOnboardingStatus.STARTED, NOW, null
			));
		}
		return repository;
	}

	private static final class FakeRefreshTokenHasher implements RefreshTokenHasher {
		@Override
		public String hash(final String refreshToken) {
			return "hash-" + refreshToken;
		}
	}

	private static final class FakeTokenIssuer implements TokenIssuer {
		@Override
		public IssuedTokens issue(final TokenIssueRequest request) {
			return new IssuedTokens("access-new", "refresh-new", "hash-refresh-new", 3600, 1209600);
		}
	}

	private static final class FakeRefreshTokenRepository implements RefreshTokenRepository {
		private final Map<Long, RefreshToken> tokens = new HashMap<>();
		private long nextId = 2L;
		private boolean failRevoke;

		@Override
		public RefreshToken save(final RefreshToken model) {
			Long id = model.id() == null ? nextId++ : model.id();
			RefreshToken saved = new RefreshToken(id, model.userId(), model.tokenHash(), model.expiresAt(), model.revokedAt());
			tokens.put(id, saved);
			return saved;
		}

		@Override
		public Optional<RefreshToken> findByTokenHash(final String tokenHash) {
			return tokens.values().stream().filter(token -> token.tokenHash().equals(tokenHash)).findFirst();
		}

		@Override
		public boolean revokeByTokenHashIfActive(
				final String tokenHash, final Long userId, final LocalDateTime revokedAt
		) {
			if (failRevoke) {
				return false;
			}
			Optional<RefreshToken> found = findByTokenHash(tokenHash)
					.filter(token -> token.userId().equals(userId))
					.filter(token -> token.revokedAt() == null)
					.filter(token -> token.expiresAt().isAfter(NOW));
			found.ifPresent(token -> tokens.put(token.id(), new RefreshToken(
					token.id(), token.userId(), token.tokenHash(), token.expiresAt(), revokedAt
			)));
			return found.isPresent();
		}

		private void put(final RefreshToken token) {
			tokens.put(token.id(), token);
		}
	}

	private static final class FakeUserAccountRepository implements UserAccountRepository {
		private final Map<Long, UserAccount> users = new HashMap<>();

		@Override
		public UserAccount save(final UserAccount model) {
			users.put(model.id(), model);
			return model;
		}

		@Override
		public Optional<UserAccount> findById(final Long id) {
			return Optional.ofNullable(users.get(id));
		}
	}
}
