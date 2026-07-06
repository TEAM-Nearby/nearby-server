// 카카오 로그인에 필요한 사용자 인증 저장소 어댑터 동작을 검증하는 테스트
package com.sopt.nearby.user.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.user.adapter.out.persistence.entity.RefreshTokenEntity;
import com.sopt.nearby.user.adapter.out.persistence.entity.SocialAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.entity.UserAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.repository.RefreshTokenJpaRepository;
import com.sopt.nearby.user.adapter.out.persistence.repository.SocialAccountJpaRepository;
import com.sopt.nearby.user.adapter.out.persistence.repository.UserAccountJpaRepository;
import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.exception.SocialAccountAlreadyExistsException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class UserAuthPersistenceAdapterTest {

	@Autowired
	private UserAccountJpaRepository userAccountJpaRepository;

	@Autowired
	private SocialAccountJpaRepository socialAccountJpaRepository;

	@Autowired
	private RefreshTokenJpaRepository refreshTokenJpaRepository;

	@Test
	void findsSocialAccountByProviderAndProviderUserId() {
		UserAccount user = userAdapter().save(newUser());
		SocialAccountRepositoryAdapter adapter = new SocialAccountRepositoryAdapter(socialAccountJpaRepository);
		SocialAccount socialAccount = adapter.save(new SocialAccount(null, user.id(), "KAKAO", "kakao-subject"));

		assertThat(adapter.findByProviderAndProviderUserId("KAKAO", "kakao-subject"))
				.contains(socialAccount);
		assertThat(adapter.findByProviderAndProviderUserId("KAKAO", "other-subject"))
				.isEmpty();
	}

	@Test
	void preventsDuplicatedSocialAccountForSameProviderUser() {
		UserAccount user = userAdapter().save(newUser());
		SocialAccountRepositoryAdapter adapter = new SocialAccountRepositoryAdapter(socialAccountJpaRepository);
		adapter.save(new SocialAccount(null, user.id(), "KAKAO", "kakao-subject"));

		assertThatThrownBy(() -> adapter.save(new SocialAccount(null, user.id(), "KAKAO", "kakao-subject")))
				.isInstanceOf(SocialAccountAlreadyExistsException.class)
				.hasCauseInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findsRefreshTokenByTokenHash() {
		UserAccount user = userAdapter().save(newUser());
		RefreshTokenRepositoryAdapter adapter = new RefreshTokenRepositoryAdapter(refreshTokenJpaRepository);
		RefreshToken refreshToken = adapter.save(new RefreshToken(
				null,
				user.id(),
				"refresh-token-hash",
				LocalDateTime.of(2026, 7, 17, 12, 0),
				null
		));

		assertThat(adapter.findByTokenHash("refresh-token-hash")).contains(refreshToken);
		assertThat(adapter.findByTokenHash("missing")).isEmpty();
	}

	@Test
	void revokesRefreshTokenOnlyWhenItIsActiveAndOwnedByUser() {
		UserAccount user = userAdapter().save(newUser());
		RefreshTokenRepositoryAdapter adapter = new RefreshTokenRepositoryAdapter(refreshTokenJpaRepository);
		RefreshToken refreshToken = adapter.save(new RefreshToken(
				null,
				user.id(),
				"refresh-token-hash",
				LocalDateTime.of(2026, 7, 17, 12, 0),
				null
		));
		LocalDateTime revokedAt = LocalDateTime.of(2026, 7, 7, 12, 0);

		boolean revoked = adapter.revokeByTokenHashIfActive("refresh-token-hash", user.id(), revokedAt);

		assertThat(revoked).isTrue();
		assertThat(adapter.findByTokenHash(refreshToken.tokenHash())).get()
				.extracting(RefreshToken::revokedAt)
				.isEqualTo(revokedAt);
	}

	@Test
	void doesNotRevokeRefreshTokenWhenAlreadyRevokedOrUserDoesNotMatch() {
		UserAccount user = userAdapter().save(newUser());
		RefreshTokenRepositoryAdapter adapter = new RefreshTokenRepositoryAdapter(refreshTokenJpaRepository);
		LocalDateTime firstRevokedAt = LocalDateTime.of(2026, 7, 7, 11, 0);
		RefreshToken refreshToken = adapter.save(new RefreshToken(
				null,
				user.id(),
				"refresh-token-hash",
				LocalDateTime.of(2026, 7, 17, 12, 0),
				firstRevokedAt
		));

		boolean revokedAgain = adapter.revokeByTokenHashIfActive(
				"refresh-token-hash",
				user.id(),
				LocalDateTime.of(2026, 7, 7, 12, 0)
		);
		boolean revokedByAnotherUser = adapter.revokeByTokenHashIfActive(
				"refresh-token-hash",
				99L,
				LocalDateTime.of(2026, 7, 7, 12, 0)
		);

		assertThat(revokedAgain).isFalse();
		assertThat(revokedByAnotherUser).isFalse();
		assertThat(adapter.findByTokenHash(refreshToken.tokenHash())).get()
				.extracting(RefreshToken::revokedAt)
				.isEqualTo(firstRevokedAt);
	}

	private UserAccountRepositoryAdapter userAdapter() {
		return new UserAccountRepositoryAdapter(userAccountJpaRepository);
	}

	private UserAccount newUser() {
		return new UserAccount(
				null,
				UserRole.USER,
				UserAccountStatus.ACTIVE,
				null,
				null,
				UserOnboardingStatus.STARTED,
				LocalDateTime.of(2026, 7, 3, 12, 0),
				null
		);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = {
			UserAccountEntity.class,
			SocialAccountEntity.class,
			RefreshTokenEntity.class
	})
	@EnableJpaRepositories(basePackageClasses = {
			UserAccountJpaRepository.class,
			SocialAccountJpaRepository.class,
			RefreshTokenJpaRepository.class
	})
	static class TestApplication {
	}
}
