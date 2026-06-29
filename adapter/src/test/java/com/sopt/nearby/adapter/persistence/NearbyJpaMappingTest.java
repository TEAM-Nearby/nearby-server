// ERD 기반 JPA 엔티티와 Spring Data Repository 구성을 검증하는 테스트
package com.sopt.nearby.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionApplicationEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionMatchEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionMeetingEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionPostEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionProfileEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionReportEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionReviewEntity;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionApplicationJpaRepository;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionPostJpaRepository;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionReportJpaRepository;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionReviewJpaRepository;
import com.sopt.nearby.adapter.persistence.place.entity.PlaceCacheEntity;
import com.sopt.nearby.adapter.persistence.place.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.adapter.persistence.user.entity.UserAccountEntity;
import com.sopt.nearby.adapter.persistence.user.repository.UserAccountJpaRepository;
import com.sopt.nearby.domain.user.model.UserAccountStatus;
import com.sopt.nearby.domain.user.model.UserOnboardingStatus;
import com.sopt.nearby.domain.user.model.UserRole;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class NearbyJpaMappingTest {

	@Autowired
	private org.springframework.context.ApplicationContext applicationContext;

	@Autowired
	private UserAccountJpaRepository userAccountJpaRepository;

	@Test
	void loadsJpaMappingsAndRepositories() {
		assertThat(applicationContext.getBean(UserAccountJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(PlaceCacheJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionProfileJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionPostJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionApplicationJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionMatchJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionMeetingJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionReportJpaRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(CompanionReviewJpaRepository.class)).isNotNull();
	}

	@Test
	void persistsUserAccountWithEnumStringColumns() {
		UserAccountEntity entity = new UserAccountEntity(
				null,
				UserRole.USER,
				UserAccountStatus.ACTIVE,
				"01012345678",
				null,
				UserOnboardingStatus.STARTED,
				LocalDateTime.now(),
				null
		);

		UserAccountEntity saved = userAccountJpaRepository.saveAndFlush(entity);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getRole()).isEqualTo(UserRole.USER);
		assertThat(saved.getStatus()).isEqualTo(UserAccountStatus.ACTIVE);
		assertThat(saved.getOnboardingStatus()).isEqualTo(UserOnboardingStatus.STARTED);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = {
			UserAccountEntity.class,
			PlaceCacheEntity.class,
			CompanionProfileEntity.class,
			CompanionPostEntity.class,
			CompanionApplicationEntity.class,
			CompanionMatchEntity.class,
			CompanionMeetingEntity.class,
			CompanionReportEntity.class,
			CompanionReviewEntity.class
	})
	@EnableJpaRepositories(basePackages = "com.sopt.nearby.adapter.persistence")
	static class TestApplication {
	}
}
