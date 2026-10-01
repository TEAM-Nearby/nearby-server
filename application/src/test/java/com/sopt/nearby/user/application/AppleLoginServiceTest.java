// 애플 로그인 유스케이스의 회원 조회와 토큰 저장 동작을 검증하는 테스트
package com.sopt.nearby.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import com.sopt.nearby.user.port.out.SocialAccountRepository;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AppleLoginServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-03T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void reusesAppleAccountAndStoresNearbyRefreshToken() {
		UserAccount existingUser = new UserAccount(
				1L,
				UserRole.USER,
				UserAccountStatus.ACTIVE,
				null,
				null,
				UserOnboardingStatus.PHONE_VERIFIED,
				LocalDateTime.now(CLOCK),
				null
		);
		AtomicReference<String> searchedProvider = new AtomicReference<>();
		AtomicReference<RefreshToken> savedRefreshToken = new AtomicReference<>();

		AppleLoginService service = new AppleLoginService(
				(idToken, nonce) -> new VerifiedUser("apple-subject"),
				request -> new IssuedTokens("access-token", "refresh-token", "refresh-hash", 3600, 1209600),
				userAccounts(existingUser),
				socialAccounts(existingUser.id(), searchedProvider),
				refreshTokens(savedRefreshToken),
				CLOCK
		);

		AppleLoginResult result = service.login(new AppleLoginCommand("id-token", "nonce"));

		assertEquals("APPLE", searchedProvider.get());
		assertEquals(existingUser.id(), result.userId());
		assertEquals("access-token", result.accessToken());
		assertEquals("refresh-hash", savedRefreshToken.get().tokenHash());
		assertEquals(LocalDateTime.of(2026, 7, 17, 12, 0), savedRefreshToken.get().expiresAt());
	}

	private UserAccountRepository userAccounts(final UserAccount existingUser) {
		return new UserAccountRepository() {
			@Override
			public Optional<UserAccount> findByIdForUpdate(final Long id) {
				return findById(id);
			}

			@Override
			public UserAccount save(final UserAccount model) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<UserAccount> findById(final Long id) {
				return Optional.of(existingUser).filter(user -> user.id().equals(id));
			}
		};
	}

	private SocialAccountRepository socialAccounts(
			final Long userId,
			final AtomicReference<String> searchedProvider
	) {
		return new SocialAccountRepository() {
			@Override
			public SocialAccount save(final SocialAccount model) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<SocialAccount> findById(final Long id) {
				return Optional.empty();
			}

			@Override
			public Optional<SocialAccount> findByProviderAndProviderUserId(
					final String provider,
					final String providerUserId
			) {
				searchedProvider.set(provider);
				return Optional.of(new SocialAccount(1L, userId, provider, providerUserId));
			}
		};
	}

	private RefreshTokenRepository refreshTokens(final AtomicReference<RefreshToken> savedRefreshToken) {
		return new RefreshTokenRepository() {
			@Override
			public RefreshToken save(final RefreshToken model) {
				savedRefreshToken.set(model);
				return model;
			}

			@Override
			public Optional<RefreshToken> findByTokenHash(final String tokenHash) {
				return Optional.empty();
			}

			@Override
			public boolean revokeByTokenHashIfActive(
					final String tokenHash,
					final Long userId,
					final LocalDateTime revokedAt
			) {
				throw new UnsupportedOperationException();
			}
		};
	}
}
