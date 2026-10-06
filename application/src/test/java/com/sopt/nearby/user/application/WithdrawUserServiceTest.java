// 회원 탈퇴 서비스의 소셜 연동 해제와 개인정보 정리를 검증하는 테스트
package com.sopt.nearby.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sopt.nearby.user.domain.model.AppleRefreshToken;
import com.sopt.nearby.user.domain.model.EmergencyContact;
import com.sopt.nearby.user.domain.model.PhoneVerification;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.domain.model.WithdrawalProgress;
import com.sopt.nearby.user.exception.AppleReauthenticationRequiredException;
import com.sopt.nearby.user.exception.SocialAccountUnlinkFailedException;
import com.sopt.nearby.user.port.out.AppleOAuthClient;
import com.sopt.nearby.user.port.out.AppleRefreshTokenRepository;
import com.sopt.nearby.user.port.out.EmergencyContactRepository;
import com.sopt.nearby.user.port.out.PhoneVerificationRepository;
import com.sopt.nearby.user.port.out.SocialAccountRepository;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import com.sopt.nearby.user.port.out.WithdrawalProgressRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class WithdrawUserServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-06T00:00:00Z"), ZoneOffset.UTC);

	@Test
	void unlinksKakaoAndCleansPersonalData() {
		Fixture fixture = new Fixture("KAKAO");
		AtomicReference<String> unlinkedKakaoId = new AtomicReference<>();
		WithdrawUserService service = fixture.service(unlinkedKakaoId::set);

		WithdrawUserResult result = service.withdraw(new WithdrawUserCommand(7L));

		assertTrue(result.withdrawn());
		assertEquals("provider-user-id", unlinkedKakaoId.get());
		UserAccount withdrawn = fixture.users.findById(7L).orElseThrow();
		assertEquals(UserAccountStatus.WITHDRAWN, withdrawn.status());
		assertNull(withdrawn.phoneNumber());
		assertEquals(LocalDateTime.of(2026, 10, 6, 0, 0), withdrawn.deletedAt());
		assertTrue(fixture.socialAccounts.findAllByUserId(7L).isEmpty());
		assertTrue(fixture.emergencyContacts.deleted);
		assertTrue(fixture.phoneVerifications.deleted);
	}

	@Test
	void revokesStoredAppleRefreshToken() {
		Fixture fixture = new Fixture("APPLE");
		fixture.appleTokens.save(new AppleRefreshToken(7L, "apple-refresh", LocalDateTime.now(CLOCK)));
		AtomicReference<String> revokedToken = new AtomicReference<>();
		fixture.appleOAuthClient.revokedToken = revokedToken;

		fixture.service(providerUserId -> {
			throw new UnsupportedOperationException();
		}).withdraw(new WithdrawUserCommand(7L));

		assertEquals("apple-refresh", revokedToken.get());
		assertFalse(fixture.appleTokens.findByUserId(7L).isPresent());
	}

	@Test
	void requiresAppleLoginAgainWhenRefreshTokenWasNotStored() {
		Fixture fixture = new Fixture("APPLE");

		assertThrows(
				AppleReauthenticationRequiredException.class,
				() -> fixture.service(providerUserId -> {
				}).withdraw(new WithdrawUserCommand(7L))
		);
		assertEquals(UserAccountStatus.ACTIVE, fixture.users.findById(7L).orElseThrow().status());
		assertFalse(fixture.socialAccounts.findAllByUserId(7L).isEmpty());
	}

	@Test
	void recordsUncertainKakaoFailureWithoutRepeatingProviderCall() {
		Fixture fixture = new Fixture("KAKAO");
		AtomicReference<Integer> calls = new AtomicReference<>(0);
		WithdrawUserService service = fixture.service(id -> {
			calls.set(calls.get() + 1);
			throw new SocialAccountUnlinkFailedException();
		});

		assertThrows(SocialAccountUnlinkFailedException.class,
				() -> service.withdraw(new WithdrawUserCommand(7L)));
		assertThrows(SocialAccountUnlinkFailedException.class,
				() -> service.withdraw(new WithdrawUserCommand(7L)));
		assertEquals(1, calls.get());
		assertEquals(UserAccountStatus.WITHDRAWING, fixture.users.findById(7L).orElseThrow().status());
		assertEquals(WithdrawalProgress.State.UNKNOWN,
				fixture.progress.findByUserIdAndProvider(7L, "KAKAO").orElseThrow().state());
		assertFalse(fixture.socialAccounts.findAllByUserId(7L).isEmpty());
		assertFalse(fixture.emergencyContacts.deleted);
	}

	@Test
	void recordsUncertainAppleRevokeFailureAndKeepsPersonalData() {
		Fixture fixture = new Fixture("APPLE");
		fixture.appleTokens.save(new AppleRefreshToken(7L, "apple-refresh", LocalDateTime.now(CLOCK)));
		fixture.appleOAuthClient.failRevoke = true;

		assertThrows(SocialAccountUnlinkFailedException.class,
				() -> fixture.service(id -> {}).withdraw(new WithdrawUserCommand(7L)));
		assertEquals(UserAccountStatus.WITHDRAWING, fixture.users.findById(7L).orElseThrow().status());
		assertEquals(WithdrawalProgress.State.UNKNOWN,
				fixture.progress.findByUserIdAndProvider(7L, "APPLE").orElseThrow().state());
		assertTrue(fixture.appleTokens.findByUserId(7L).isPresent());
		assertFalse(fixture.phoneVerifications.deleted);
	}

	private static final class Fixture {
		private final FakeUserAccountRepository users = new FakeUserAccountRepository();
		private final FakeSocialAccountRepository socialAccounts = new FakeSocialAccountRepository();
		private final FakeAppleRefreshTokenRepository appleTokens = new FakeAppleRefreshTokenRepository();
		private final FakeEmergencyContactRepository emergencyContacts = new FakeEmergencyContactRepository();
		private final FakePhoneVerificationRepository phoneVerifications = new FakePhoneVerificationRepository();
		private final FakeAppleOAuthClient appleOAuthClient = new FakeAppleOAuthClient();
		private final FakeWithdrawalProgressRepository progress = new FakeWithdrawalProgressRepository();

		private Fixture(final String provider) {
			users.save(new UserAccount(
					7L,
					UserRole.USER,
					UserAccountStatus.ACTIVE,
					"01012345678",
					LocalDateTime.now(CLOCK),
					UserOnboardingStatus.COMPANION_PROFILE_COMPLETED,
					LocalDateTime.now(CLOCK).minusDays(10),
					null
			));
			socialAccounts.save(new SocialAccount(1L, 7L, provider, "provider-user-id"));
		}

		private WithdrawUserService service(
				final com.sopt.nearby.user.port.out.KakaoAccountUnlinker kakaoAccountUnlinker
		) {
			return new WithdrawUserService(
					users,
					socialAccounts,
					appleTokens,
					emergencyContacts,
					phoneVerifications,
					kakaoAccountUnlinker,
					appleOAuthClient,
					progress,
					event -> {
					},
					CLOCK
			);
		}
	}

	private static final class FakeWithdrawalProgressRepository implements WithdrawalProgressRepository {
		private final Map<String, WithdrawalProgress> values = new HashMap<>();

		@Override
		public WithdrawalProgress save(final WithdrawalProgress progress) {
			values.put(progress.userId() + ":" + progress.provider(), progress);
			return progress;
		}

		@Override
		public Optional<WithdrawalProgress> findByUserIdAndProvider(final Long userId, final String provider) {
			return Optional.ofNullable(values.get(userId + ":" + provider));
		}

		@Override
		public java.util.List<WithdrawalProgress> findAllByUserId(final Long userId) {
			return values.values().stream().filter(value -> value.userId().equals(userId)).toList();
		}

		@Override
		public java.util.List<Long> findResumableUserIds(final int limit) {
			return java.util.List.of();
		}

		@Override
		public void deleteByUserId(final Long userId) {
			values.values().removeIf(value -> value.userId().equals(userId));
		}
	}

	private static final class FakeUserAccountRepository implements UserAccountRepository {
		private final Map<Long, UserAccount> values = new HashMap<>();

		@Override
		public UserAccount save(final UserAccount model) {
			values.put(model.id(), model);
			return model;
		}

		@Override
		public Optional<UserAccount> findById(final Long id) {
			return Optional.ofNullable(values.get(id));
		}

		@Override
		public Optional<UserAccount> findByIdForUpdate(final Long id) {
			return findById(id);
		}
	}

	private static final class FakeSocialAccountRepository implements SocialAccountRepository {
		private final Map<Long, SocialAccount> values = new HashMap<>();

		@Override
		public SocialAccount save(final SocialAccount model) {
			values.put(model.id(), model);
			return model;
		}

		@Override
		public Optional<SocialAccount> findById(final Long id) {
			return Optional.ofNullable(values.get(id));
		}

		@Override
		public Optional<SocialAccount> findByProviderAndProviderUserId(
				final String provider,
				final String providerUserId
		) {
			return values.values().stream()
					.filter(account -> account.provider().equals(provider))
					.filter(account -> account.providerUserId().equals(providerUserId))
					.findFirst();
		}

		@Override
		public java.util.List<SocialAccount> findAllByUserId(final Long userId) {
			return values.values().stream().filter(account -> account.userId().equals(userId)).toList();
		}

		@Override
		public void deleteByUserId(final Long userId) {
			values.values().removeIf(account -> account.userId().equals(userId));
		}
	}

	private static final class FakeAppleRefreshTokenRepository implements AppleRefreshTokenRepository {
		private final Map<Long, AppleRefreshToken> values = new HashMap<>();

		@Override
		public AppleRefreshToken save(final AppleRefreshToken token) {
			values.put(token.userId(), token);
			return token;
		}

		@Override
		public Optional<AppleRefreshToken> findByUserId(final Long userId) {
			return Optional.ofNullable(values.get(userId));
		}

		@Override
		public void deleteByUserId(final Long userId) {
			values.remove(userId);
		}
	}

	private static final class FakeEmergencyContactRepository implements EmergencyContactRepository {
		private boolean deleted;

		@Override
		public EmergencyContact save(final EmergencyContact model) {
			return model;
		}

		@Override
		public Optional<EmergencyContact> findById(final Long id) {
			return Optional.empty();
		}

		@Override
		public Optional<EmergencyContact> findByUserId(final Long userId) {
			return Optional.empty();
		}

		@Override
		public void deleteByUserId(final Long userId) {
			deleted = true;
		}
	}

	private static final class FakePhoneVerificationRepository implements PhoneVerificationRepository {
		private boolean deleted;

		@Override
		public PhoneVerification save(final PhoneVerification model) {
			return model;
		}

		@Override
		public Optional<PhoneVerification> findById(final Long id) {
			return Optional.empty();
		}

		@Override
		public void deleteByUserId(final Long userId) {
			deleted = true;
		}
	}

	private static final class FakeAppleOAuthClient implements AppleOAuthClient {
		private AtomicReference<String> revokedToken = new AtomicReference<>();
		private boolean failRevoke;

		@Override
		public Tokens exchangeAuthorizationCode(final String authorizationCode) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void revoke(final String refreshToken) {
			if (failRevoke) {
				throw new SocialAccountUnlinkFailedException();
			}
			revokedToken.set(refreshToken);
		}
	}
}
