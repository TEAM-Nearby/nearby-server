// 탈퇴 진행 상태의 영속화와 재개 대상 조회를 검증하는 테스트
package com.sopt.nearby.user.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.user.adapter.out.persistence.entity.UserAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.entity.WithdrawalProgressEntity;
import com.sopt.nearby.user.adapter.out.persistence.repository.UserAccountJpaRepository;
import com.sopt.nearby.user.adapter.out.persistence.repository.WithdrawalProgressJpaRepository;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.domain.model.WithdrawalProgress;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class WithdrawalPersistenceAdapterTest {
	@Autowired private UserAccountJpaRepository users;
	@Autowired private WithdrawalProgressJpaRepository progress;

	@Test
	void storesProgressAndFindsOnlySafeRecoveryCandidates() {
		Long userId = new UserAccountRepositoryAdapter(users).save(new UserAccount(null, UserRole.USER,
				UserAccountStatus.WITHDRAWING, null, null, UserOnboardingStatus.STARTED,
				LocalDateTime.of(2026, 10, 6, 0, 0), null)).id();
		WithdrawalProgressRepositoryAdapter adapter = new WithdrawalProgressRepositoryAdapter(progress);

		adapter.save(new WithdrawalProgress(userId, "APPLE", WithdrawalProgress.State.IN_FLIGHT));
		assertThat(adapter.findByUserIdAndProvider(userId, "APPLE").orElseThrow().state())
				.isEqualTo(WithdrawalProgress.State.IN_FLIGHT);
		assertThat(adapter.findResumableUserIds(50)).isEmpty();
		adapter.save(new WithdrawalProgress(userId, "APPLE", WithdrawalProgress.State.SUCCEEDED));
		assertThat(adapter.findAllByUserId(userId)).extracting(WithdrawalProgress::state)
				.containsExactly(WithdrawalProgress.State.SUCCEEDED);
		assertThat(adapter.findResumableUserIds(50)).containsExactly(userId);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = {UserAccountEntity.class, WithdrawalProgressEntity.class})
	@EnableJpaRepositories(basePackageClasses = {UserAccountJpaRepository.class, WithdrawalProgressJpaRepository.class})
	static class TestApplication {
	}
}
