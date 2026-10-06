// Apple Refresh Token 암호화 저장과 변조 거부를 검증하는 테스트
package com.sopt.nearby.user.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.user.adapter.out.persistence.entity.AppleRefreshTokenEntity;
import com.sopt.nearby.user.adapter.out.persistence.entity.UserAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.repository.AppleRefreshTokenJpaRepository;
import com.sopt.nearby.user.adapter.out.persistence.repository.UserAccountJpaRepository;
import com.sopt.nearby.user.domain.model.AppleRefreshToken;
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
class AppleRefreshTokenPersistenceAdapterTest {
	@Autowired private UserAccountJpaRepository users;
	@Autowired private AppleRefreshTokenJpaRepository tokens;

	@Test
	void storesEncryptedTokenAndReadsPlaintextThroughAdapter() {
		Long userId = new UserAccountRepositoryAdapter(users).save(new UserAccount(null, UserRole.USER,
				UserAccountStatus.ACTIVE, null, null, UserOnboardingStatus.STARTED,
				LocalDateTime.of(2026, 10, 6, 0, 0), null)).id();
		AppleRefreshTokenCipher cipher = new AppleRefreshTokenCipher("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
		AppleRefreshTokenRepositoryAdapter adapter = new AppleRefreshTokenRepositoryAdapter(tokens, cipher);
		AppleRefreshToken token = new AppleRefreshToken(userId, "sensitive-refresh-token",
				LocalDateTime.of(2026, 10, 6, 0, 0));

		adapter.save(token);
		assertThat(tokens.findById(userId).orElseThrow().getRefreshToken())
				.startsWith("v1:").doesNotContain("sensitive-refresh-token");
		assertThat(adapter.findByUserId(userId)).contains(token);
		adapter.deleteByUserId(userId);
		assertThat(adapter.findByUserId(userId)).isEmpty();
	}

	@Test
	void rejectsTamperedCiphertext() {
		AppleRefreshTokenCipher cipher = new AppleRefreshTokenCipher("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
		String encrypted = cipher.encrypt("refresh-token");
		String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

		assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = {UserAccountEntity.class, AppleRefreshTokenEntity.class})
	@EnableJpaRepositories(basePackageClasses = {UserAccountJpaRepository.class, AppleRefreshTokenJpaRepository.class})
	static class TestApplication {
	}
}
