// 탈퇴 회원의 동행 프로필 개인정보 익명화를 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.user.port.in.UserWithdrawnEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnonymizeWithdrawnUserProfileListenerTest {

	@Test
	void anonymizesPersonalProfileFieldsAndKeepsReviewHistory() {
		FakeCompanionProfileRepository profiles = new FakeCompanionProfileRepository(new CompanionProfile(
				1L,
				7L,
				"니어바이",
				UserGender.FEMALE,
				2000,
				"https://example.com/profile.png",
				"같이 식사해요",
				new BigDecimal("4.50"),
				3,
				CompanionProfileStatus.ACTIVE
		));

		new AnonymizeWithdrawnUserProfileListener(profiles).anonymize(new UserWithdrawnEvent(7L));

		CompanionProfile anonymized = profiles.profile;
		assertEquals("탈퇴한 사용자-7", anonymized.nickname());
		assertEquals(UserGender.FEMALE, anonymized.gender());
		assertNull(anonymized.birthYear());
		assertNull(anonymized.profileImageUrl());
		assertNull(anonymized.intro());
		assertEquals(new BigDecimal("4.50"), anonymized.mannerScore());
		assertEquals(3, anonymized.reviewCount());
		assertEquals(CompanionProfileStatus.INACTIVE, anonymized.status());
	}

	private static final class FakeCompanionProfileRepository implements CompanionProfileRepository {
		private CompanionProfile profile;

		private FakeCompanionProfileRepository(final CompanionProfile profile) {
			this.profile = profile;
		}

		@Override
		public CompanionProfile save(final CompanionProfile model) {
			profile = model;
			return model;
		}

		@Override
		public Optional<CompanionProfile> findById(final Long id) {
			return Optional.ofNullable(profile).filter(found -> found.id().equals(id));
		}

		@Override
		public List<CompanionProfile> findAllByUserIdIn(final List<Long> userIds) {
			return userIds.contains(profile.userId()) ? List.of(profile) : List.of();
		}

		@Override
		public boolean existsByNickname(final String nickname) {
			return profile.nickname().equals(nickname);
		}

		@Override
		public boolean existsByUserId(final Long userId) {
			return profile.userId().equals(userId);
		}

		@Override
		public Optional<CompanionProfile> findByUserId(final Long userId) {
			return Optional.ofNullable(profile).filter(found -> found.userId().equals(userId));
		}

		@Override
		public Optional<CompanionProfile> findByUserIdForUpdate(final Long userId) {
			return findByUserId(userId);
		}
	}
}
