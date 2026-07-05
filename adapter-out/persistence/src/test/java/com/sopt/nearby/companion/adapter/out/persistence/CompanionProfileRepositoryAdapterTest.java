// 동행 프로필 저장소 어댑터의 사용자 ID 목록 조회와 도메인 매핑을 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionProfileRepositoryAdapterTest {

	@Autowired
	private CompanionProfileJpaRepository companionProfileJpaRepository;

	@Test
	void findsProfilesByUserIdsAndMapsEntitiesToDomainModels() {
		CompanionProfileRepositoryAdapter adapter = new CompanionProfileRepositoryAdapter(companionProfileJpaRepository);
		companionProfileJpaRepository.saveAndFlush(profile(
				7L,
				"여행자A",
				UserGender.FEMALE,
				1998,
				"https://image.example/a.png",
				"저녁 동행을 좋아해요.",
				new BigDecimal("4.50"),
				3,
				CompanionProfileStatus.ACTIVE
		));
		companionProfileJpaRepository.saveAndFlush(profile(
				8L,
				"여행자B",
				UserGender.MALE,
				null,
				null,
				null,
				new BigDecimal("3.75"),
				0,
				CompanionProfileStatus.ACTIVE
		));
		companionProfileJpaRepository.saveAndFlush(profile(
				9L,
				"제외 대상",
				UserGender.MALE,
				2000,
				"https://image.example/excluded.png",
				"조회 대상이 아니에요.",
				new BigDecimal("5.00"),
				7,
				CompanionProfileStatus.INACTIVE
		));

		List<CompanionProfileEntity> entities = companionProfileJpaRepository.findAllByUserIdIn(List.of(7L, 8L));
		List<CompanionProfile> profiles = adapter.findAllByUserIdIn(List.of(7L, 8L));

		assertThat(entities)
				.extracting(CompanionProfileEntity::getUserId)
				.containsExactlyInAnyOrder(7L, 8L);
		assertThat(profiles)
				.extracting(CompanionProfile::userId)
				.containsExactlyInAnyOrder(7L, 8L);

		CompanionProfile firstProfile = profiles.stream()
				.filter(profile -> profile.userId().equals(7L))
				.findFirst()
				.orElseThrow();
		assertThat(firstProfile.nickname()).isEqualTo("여행자A");
		assertThat(firstProfile.gender()).isEqualTo(UserGender.FEMALE);
		assertThat(firstProfile.birthYear()).isEqualTo(1998);
		assertThat(firstProfile.profileImageUrl()).isEqualTo("https://image.example/a.png");
		assertThat(firstProfile.intro()).isEqualTo("저녁 동행을 좋아해요.");
		assertThat(firstProfile.mannerScore()).isEqualByComparingTo("4.50");
		assertThat(firstProfile.reviewCount()).isEqualTo(3);
		assertThat(firstProfile.status()).isEqualTo(CompanionProfileStatus.ACTIVE);
		assertThat(adapter.existsByNickname("여행자A")).isTrue();
		assertThat(adapter.existsByNickname("없는닉네임")).isFalse();
		assertThat(adapter.existsByUserId(7L)).isTrue();
		assertThat(adapter.existsByUserId(99L)).isFalse();
		assertThat(adapter.findByUserId(7L)).get()
				.extracting(CompanionProfile::nickname)
				.isEqualTo("여행자A");
	}

	private CompanionProfileEntity profile(
			final Long userId,
			final String nickname,
			final UserGender gender,
			final Integer birthYear,
			final String profileImageUrl,
			final String intro,
			final BigDecimal mannerScore,
			final int reviewCount,
			final CompanionProfileStatus status
	) {
		return new CompanionProfileEntity(
				null,
				userId,
				nickname,
				gender,
				birthYear,
				profileImageUrl,
				intro,
				mannerScore,
				reviewCount,
				status
		);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = CompanionProfileEntity.class)
	@EnableJpaRepositories(basePackageClasses = CompanionProfileJpaRepository.class)
	static class TestApplication {
	}
}
