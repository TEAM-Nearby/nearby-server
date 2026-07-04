// 휴대폰 인증 저장소 어댑터의 인증 코드 해시 저장을 검증하는 테스트
package com.sopt.nearby.user.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.user.adapter.out.persistence.entity.PhoneVerificationEntity;
import com.sopt.nearby.user.adapter.out.persistence.entity.UserAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.repository.PhoneVerificationJpaRepository;
import com.sopt.nearby.user.adapter.out.persistence.repository.UserAccountJpaRepository;
import com.sopt.nearby.user.domain.model.PhoneVerification;
import com.sopt.nearby.user.domain.model.PhoneVerificationStatus;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class PhoneVerificationPersistenceAdapterTest {

	@Autowired
	private UserAccountJpaRepository userAccountJpaRepository;

	@Autowired
	private PhoneVerificationJpaRepository phoneVerificationJpaRepository;

	@Test
	void savesAndFindsVerificationCodeHash() {
		UserAccount user = userAdapter().save(newUser());
		PhoneVerificationRepositoryAdapter adapter = new PhoneVerificationRepositoryAdapter(
				phoneVerificationJpaRepository
		);

		PhoneVerification saved = adapter.save(new PhoneVerification(
				null,
				user.id(),
				"01012345678",
				null,
				"code-hash",
				PhoneVerificationStatus.PENDING,
				LocalDateTime.of(2026, 7, 4, 7, 3),
				null
		));

		assertThat(adapter.findById(saved.id()))
				.get()
				.extracting(PhoneVerification::verificationCodeHash)
				.isEqualTo("code-hash");
	}

	@Test
	void updatesPhoneVerificationStatusAndVerifiedAt() {
		UserAccount user = userAdapter().save(newUser());
		PhoneVerificationRepositoryAdapter adapter = new PhoneVerificationRepositoryAdapter(
				phoneVerificationJpaRepository
		);
		PhoneVerification saved = adapter.save(new PhoneVerification(
				null,
				user.id(),
				"01012345678",
				null,
				"code-hash",
				PhoneVerificationStatus.PENDING,
				LocalDateTime.of(2026, 7, 4, 7, 3),
				null
		));

		adapter.save(new PhoneVerification(
				saved.id(),
				saved.userId(),
				saved.phoneNumber(),
				saved.carrier(),
				saved.verificationCodeHash(),
				PhoneVerificationStatus.VERIFIED,
				saved.expiresAt(),
				LocalDateTime.of(2026, 7, 4, 7, 0)
		));

		assertThat(adapter.findById(saved.id()))
				.get()
				.satisfies(verification -> {
					assertThat(verification.status()).isEqualTo(PhoneVerificationStatus.VERIFIED);
					assertThat(verification.verifiedAt()).isEqualTo(LocalDateTime.of(2026, 7, 4, 7, 0));
				});
	}

	@Test
	void updatesUserPhoneVerificationFields() {
		UserAccountRepositoryAdapter adapter = userAdapter();
		UserAccount saved = adapter.save(newUser());

		adapter.save(new UserAccount(
				saved.id(),
				saved.role(),
				saved.status(),
				"01012345678",
				LocalDateTime.of(2026, 7, 4, 7, 0),
				UserOnboardingStatus.PHONE_VERIFIED,
				saved.createdAt(),
				saved.deletedAt()
		));

		assertThat(adapter.findById(saved.id()))
				.get()
				.satisfies(user -> {
					assertThat(user.phoneNumber()).isEqualTo("01012345678");
					assertThat(user.phoneVerifiedAt()).isEqualTo(LocalDateTime.of(2026, 7, 4, 7, 0));
					assertThat(user.onboardingStatus()).isEqualTo(UserOnboardingStatus.PHONE_VERIFIED);
				});
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
				LocalDateTime.of(2026, 7, 4, 7, 0),
				null
		);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = {
			UserAccountEntity.class,
			PhoneVerificationEntity.class
	})
	@EnableJpaRepositories(basePackageClasses = {
			UserAccountJpaRepository.class,
			PhoneVerificationJpaRepository.class
	})
	static class TestApplication {
	}
}
