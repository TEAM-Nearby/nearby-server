// 카카오 로그인 유스케이스의 회원 매핑과 토큰 저장 동작을 검증하는 테스트
package com.sopt.nearby.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.exception.KakaoLoginFailedException;
import com.sopt.nearby.user.exception.SocialAccountAlreadyExistsException;
import com.sopt.nearby.user.port.out.KakaoIdTokenVerifier;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import com.sopt.nearby.user.port.out.SocialAccountRepository;
import com.sopt.nearby.user.port.out.TokenIssuer;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class KakaoLoginServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-03T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void createsUserAndSocialAccountWhenKakaoAccountIsNew() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		FakeSocialAccountRepository socialAccounts = new FakeSocialAccountRepository();
		FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
		KakaoLoginService service = service(userAccounts, socialAccounts, refreshTokens, "kakao-subject");

		KakaoLoginResult result = service.login(new KakaoLoginCommand("id-token", "nonce"));

		assertEquals(1L, result.userId());
		assertEquals(UserOnboardingStatus.STARTED, result.onboardingStatus());
		assertEquals("access-1", result.accessToken());
		assertEquals("refresh-1", result.refreshToken());
		assertEquals(3600, result.accessTokenExpiresIn());
		assertEquals(1209600, result.refreshTokenExpiresIn());
		assertEquals(1, userAccounts.saved.size());
		assertTrue(socialAccounts.findByProviderAndProviderUserId("KAKAO", "kakao-subject").isPresent());
		assertEquals(1, refreshTokens.saved.size());
		RefreshToken savedRefreshToken = refreshTokens.saved.values().iterator().next();
		assertEquals(1L, savedRefreshToken.userId());
		assertEquals("refresh-hash-1", savedRefreshToken.tokenHash());
		assertEquals(LocalDateTime.of(2026, 7, 17, 12, 0), savedRefreshToken.expiresAt());
	}

	@Test
	void reusesExistingUserWhenSocialAccountAlreadyExists() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		UserAccount existing = userAccounts.save(new UserAccount(
				null,
				UserRole.USER,
				UserAccountStatus.ACTIVE,
				null,
				null,
				UserOnboardingStatus.PHONE_VERIFIED,
				LocalDateTime.now(CLOCK),
				null
		));
		FakeSocialAccountRepository socialAccounts = new FakeSocialAccountRepository();
		socialAccounts.save(new SocialAccount(null, existing.id(), "KAKAO", "kakao-subject"));
		FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
		KakaoLoginService service = service(userAccounts, socialAccounts, refreshTokens, "kakao-subject");

		KakaoLoginResult result = service.login(new KakaoLoginCommand("id-token", "nonce"));

		assertEquals(existing.id(), result.userId());
		assertEquals(UserOnboardingStatus.PHONE_VERIFIED, result.onboardingStatus());
		assertEquals(1, userAccounts.saved.size());
		assertEquals(1, socialAccounts.saved.size());
		assertEquals(1, refreshTokens.saved.size());
	}

	@Test
	void reusesExistingUserWhenSocialAccountIsCreatedConcurrently() {
		FakeUserAccountRepository userAccounts = new FakeUserAccountRepository();
		UserAccount existing = userAccounts.save(new UserAccount(
				null,
				UserRole.USER,
				UserAccountStatus.ACTIVE,
				null,
				null,
				UserOnboardingStatus.PHONE_VERIFIED,
				LocalDateTime.now(CLOCK),
				null
		));
		FakeSocialAccountRepository socialAccounts = new ConcurrentSocialAccountRepository(
				new SocialAccount(1L, existing.id(), "KAKAO", "kakao-subject")
		);
		FakeRefreshTokenRepository refreshTokens = new FakeRefreshTokenRepository();
		KakaoLoginService service = service(userAccounts, socialAccounts, refreshTokens, "kakao-subject");

		KakaoLoginResult result = service.login(new KakaoLoginCommand("id-token", "nonce"));

		assertEquals(existing.id(), result.userId());
		assertEquals(UserOnboardingStatus.PHONE_VERIFIED, result.onboardingStatus());
		assertEquals(1, refreshTokens.saved.size());
	}

	@Test
	void failsWhenKakaoIdTokenCannotBeVerified() {
		KakaoLoginService service = new KakaoLoginService(
				(idToken, nonce) -> {
					throw new KakaoLoginFailedException();
				},
				new FakeTokenIssuer(),
				new FakeUserAccountRepository(),
				new FakeSocialAccountRepository(),
				new FakeRefreshTokenRepository(),
				CLOCK
		);

		RuntimeException exception = assertThrows(
				RuntimeException.class,
				() -> service.login(new KakaoLoginCommand("bad-token", "nonce"))
		);
		assertInstanceOf(KakaoLoginFailedException.class, exception);
	}

	@Test
	void loginMethodHasTransactionBoundary() throws Exception {
		boolean hasTransactional = Arrays.stream(
						KakaoLoginService.class.getMethod("login", KakaoLoginCommand.class).getAnnotations()
				)
				.anyMatch(annotation -> annotation.annotationType()
						.getName()
						.equals("org.springframework.transaction.annotation.Transactional"));

		assertTrue(hasTransactional);
	}

	private static KakaoLoginService service(
			final FakeUserAccountRepository userAccounts,
			final FakeSocialAccountRepository socialAccounts,
			final FakeRefreshTokenRepository refreshTokens,
			final String providerUserId
	) {
		return new KakaoLoginService(
				(idToken, nonce) -> new VerifiedKakaoUser(providerUserId),
				new FakeTokenIssuer(),
				userAccounts,
				socialAccounts,
				refreshTokens,
				CLOCK
		);
	}

	private static final class FakeTokenIssuer implements TokenIssuer {

		@Override
		public IssuedTokens issue(final TokenIssueRequest request) {
			return new IssuedTokens(
					"access-" + request.userId(),
					"refresh-" + request.userId(),
					"refresh-hash-" + request.userId(),
					3600,
					1209600
			);
		}
	}

	private static final class FakeUserAccountRepository implements UserAccountRepository {

		private final Map<Long, UserAccount> accounts = new HashMap<>();
		private final Map<Long, UserAccount> saved = new HashMap<>();
		private long nextId = 1L;

		@Override
		public UserAccount save(final UserAccount model) {
			UserAccount savedAccount = new UserAccount(
					model.id() == null ? nextId++ : model.id(),
					model.role(),
					model.status(),
					model.phoneNumber(),
					model.phoneVerifiedAt(),
					model.onboardingStatus(),
					model.createdAt(),
					model.deletedAt()
			);
			accounts.put(savedAccount.id(), savedAccount);
			saved.put(savedAccount.id(), savedAccount);
			assertNotNull(savedAccount.id());
			return savedAccount;
		}

		@Override
		public Optional<UserAccount> findById(final Long id) {
			return Optional.ofNullable(accounts.get(id));
		}
	}

	private static class FakeSocialAccountRepository implements SocialAccountRepository {

		private final Map<Long, SocialAccount> saved = new HashMap<>();
		private long nextId = 1L;

		@Override
		public SocialAccount save(final SocialAccount model) {
			SocialAccount savedAccount = new SocialAccount(
					model.id() == null ? nextId++ : model.id(),
					model.userId(),
					model.provider(),
					model.providerUserId()
			);
			saved.put(savedAccount.id(), savedAccount);
			return savedAccount;
		}

		@Override
		public Optional<SocialAccount> findById(final Long id) {
			return Optional.ofNullable(saved.get(id));
		}

		@Override
		public Optional<SocialAccount> findByProviderAndProviderUserId(
				final String provider,
				final String providerUserId
		) {
			return saved.values()
					.stream()
					.filter(account -> account.provider().equals(provider))
					.filter(account -> account.providerUserId().equals(providerUserId))
					.findFirst();
		}
	}

	private static final class ConcurrentSocialAccountRepository extends FakeSocialAccountRepository {

		private final SocialAccount existingSocialAccount;
		private boolean duplicated;

		private ConcurrentSocialAccountRepository(final SocialAccount existingSocialAccount) {
			this.existingSocialAccount = existingSocialAccount;
		}

		@Override
		public SocialAccount save(final SocialAccount model) {
			duplicated = true;
			throw new SocialAccountAlreadyExistsException();
		}

		@Override
		public Optional<SocialAccount> findByProviderAndProviderUserId(
				final String provider,
				final String providerUserId
		) {
			if (!duplicated) {
				return Optional.empty();
			}
			return Optional.of(existingSocialAccount)
					.filter(account -> account.provider().equals(provider))
					.filter(account -> account.providerUserId().equals(providerUserId));
		}
	}

	private static final class FakeRefreshTokenRepository implements RefreshTokenRepository {

		private final Map<Long, RefreshToken> saved = new HashMap<>();
		private long nextId = 1L;

		@Override
		public RefreshToken save(final RefreshToken model) {
			RefreshToken savedToken = new RefreshToken(
					model.id() == null ? nextId++ : model.id(),
					model.userId(),
					model.tokenHash(),
					model.expiresAt(),
					model.revokedAt()
			);
			saved.put(savedToken.id(), savedToken);
			return savedToken;
		}

		@Override
		public Optional<RefreshToken> findByTokenHash(final String tokenHash) {
			return saved.values()
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
			throw new UnsupportedOperationException("로그인 테스트에서는 리프레시 토큰 만료 처리를 사용하지 않습니다.");
		}
	}
}
