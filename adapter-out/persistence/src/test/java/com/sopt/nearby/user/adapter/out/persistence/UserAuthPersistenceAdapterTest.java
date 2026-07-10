// 카카오 로그인에 필요한 사용자 인증 저장소 어댑터 동작을 검증하는 테스트
package com.sopt.nearby.user.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.user.adapter.out.persistence.entity.SocialAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.entity.UserAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.repository.SocialAccountJpaRepository;
import com.sopt.nearby.user.adapter.out.persistence.repository.UserAccountJpaRepository;
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
			SocialAccountEntity.class
	})
	@EnableJpaRepositories(basePackageClasses = {
			UserAccountJpaRepository.class,
			SocialAccountJpaRepository.class
	})
	static class TestApplication {
	}
}
