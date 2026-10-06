// 애플 로그인 유스케이스의 회원 조회와 토큰 저장 동작을 검증하는 테스트
package com.sopt.nearby.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.domain.model.AppleRefreshToken;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import com.sopt.nearby.user.port.out.AppleRefreshTokenRepository;
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
		AtomicReference<AppleRefreshToken> savedAppleRefreshToken = new AtomicReference<>();

		AppleLoginService service = new AppleLoginService(
				(idToken, nonce) -> new VerifiedUser("apple-subject"),
				new com.sopt.nearby.user.port.out.AppleOAuthClient() {
					@Override
					public Tokens exchangeAuthorizationCode(final String authorizationCode) {
						return new Tokens("apple-refresh-token", "exchanged-id-token");
					}

					@Override
					public void revoke(final String refreshToken) {
						throw new UnsupportedOperationException();
					}
				},
				appleRefreshTokens(savedAppleRefreshToken),
				request -> new IssuedTokens("access-token", "refresh-token", "refresh-hash", 3600, 1209600),
				userAccounts(existingUser),
				socialAccounts(existingUser.id(), searchedProvider),
				refreshTokens(savedRefreshToken),
				CLOCK
		);

		AppleLoginResult result = service.login(new AppleLoginCommand("id-token", "nonce", "authorization-code"));

		assertEquals("APPLE", searchedProvider.get());
		assertEquals(existingUser.id(), result.userId());
		assertEquals("access-token", result.accessToken());
		assertEquals("refresh-hash", savedRefreshToken.get().tokenHash());
		assertEquals(LocalDateTime.of(2026, 7, 17, 12, 0), savedRefreshToken.get().expiresAt());
		assertEquals("apple-refresh-token", savedAppleRefreshToken.get().refreshToken());
		assertEquals(existingUser.id(), savedAppleRefreshToken.get().userId());
	}

	@Test
	void rejectsMismatchedExchangedIdentityBeforeSavingToken() {
		UserAccount existingUser = new UserAccount(1L, UserRole.USER, UserAccountStatus.ACTIVE,
				null, null, UserOnboardingStatus.STARTED, LocalDateTime.now(CLOCK), null);
		AtomicReference<AppleRefreshToken> savedToken = new AtomicReference<>();
		AppleLoginService service = new AppleLoginService(
				(idToken, nonce) -> new VerifiedUser(idToken.equals("exchanged-id-token") ? "other-subject" : "apple-subject"),
				new com.sopt.nearby.user.port.out.AppleOAuthClient() {
					@Override
				public Tokens exchangeAuthorizationCode(final String code) {
						return new Tokens("refresh", "exchanged-id-token");
					}
					@Override
				public void revoke(final String token) {
					}
				},
				appleRefreshTokens(savedToken), request -> { throw new AssertionError("token issued"); },
				userAccounts(existingUser), socialAccounts(1L, new AtomicReference<>()),
				refreshTokens(new AtomicReference<>()), CLOCK
		);

		assertThrows(com.sopt.nearby.user.exception.AppleLoginFailedException.class,
				() -> service.login(new AppleLoginCommand("id-token", "nonce", "code")));
		assertNull(savedToken.get());
	}

	@Test
	void rejectsWithdrawnUserBeforeExchangingCode() {
		UserAccount withdrawn = new UserAccount(1L, UserRole.USER, UserAccountStatus.WITHDRAWN,
				null, null, UserOnboardingStatus.STARTED, LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK));
		AppleLoginService service = new AppleLoginService(
				(idToken, nonce) -> new VerifiedUser("apple-subject"),
				new com.sopt.nearby.user.port.out.AppleOAuthClient() {
					@Override
				public Tokens exchangeAuthorizationCode(final String code) {
						throw new AssertionError("code exchanged");
					}
					@Override
				public void revoke(final String token) {
					}
				},
				appleRefreshTokens(new AtomicReference<>()), request -> { throw new AssertionError("token issued"); },
				userAccounts(withdrawn), socialAccounts(1L, new AtomicReference<>()),
				refreshTokens(new AtomicReference<>()), CLOCK
		);

		assertThrows(com.sopt.nearby.user.exception.AppleLoginFailedException.class,
				() -> service.login(new AppleLoginCommand("id-token", "nonce", "code")));
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

			@Override
			public java.util.List<SocialAccount> findAllByUserId(final Long ignoredUserId) {
				return java.util.List.of();
			}

			@Override
			public void deleteByUserId(final Long ignoredUserId) {
				throw new UnsupportedOperationException();
			}
		};
	}

	private AppleRefreshTokenRepository appleRefreshTokens(
			final AtomicReference<AppleRefreshToken> savedAppleRefreshToken
	) {
		return new AppleRefreshTokenRepository() {
			@Override
			public AppleRefreshToken save(final AppleRefreshToken token) {
				savedAppleRefreshToken.set(token);
				return token;
			}

			@Override
			public Optional<AppleRefreshToken> findByUserId(final Long userId) {
				return Optional.ofNullable(savedAppleRefreshToken.get());
			}

			@Override
			public void deleteByUserId(final Long userId) {
				savedAppleRefreshToken.set(null);
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
