// 동행 프로필 등록 유스케이스를 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.DuplicateCompanionProfileException;
import com.sopt.nearby.companion.domain.exception.DuplicateNicknameException;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStyle;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileStyleRepository;
import com.sopt.nearby.user.exception.PhoneVerificationRequiredException;
import com.sopt.nearby.user.port.in.CompleteCompanionProfileOnboardingUseCase;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegisterCompanionProfileServiceTest {

	private FakeCompanionProfileRepository profileRepository;
	private FakeCompanionProfileStyleRepository styleRepository;
	private FakeCompleteCompanionProfileOnboardingUseCase completeOnboardingUseCase;
	private RegisterCompanionProfileService service;

	@BeforeEach
	void setUp() {
		profileRepository = new FakeCompanionProfileRepository();
		styleRepository = new FakeCompanionProfileStyleRepository();
		completeOnboardingUseCase = new FakeCompleteCompanionProfileOnboardingUseCase();
		service = new RegisterCompanionProfileService(
				profileRepository,
				styleRepository,
				completeOnboardingUseCase
		);
	}

	@Test
	void registersCompanionProfileAndCompletesOnboarding() {
		RegisteredCompanionProfileResult result = service.register(new RegisterCompanionProfileCommand(
				1L,
				"여행친구",
				UserGender.FEMALE,
				"혼자 여행도 같이 여행도 좋아해요",
				"https://cdn.nearby.com/profiles/1/profile.jpg",
				List.of(TravelStyleKeyword.PLANNED, TravelStyleKeyword.FOODIE)
		));

		assertEquals(5L, result.profileId());
		assertEquals("여행친구", result.nickname());
		assertEquals(UserGender.FEMALE, result.gender());
		assertEquals("혼자 여행도 같이 여행도 좋아해요", result.intro());
		assertEquals("https://cdn.nearby.com/profiles/1/profile.jpg", result.profileImageUrl());
		assertIterableEquals(
				List.of(TravelStyleKeyword.PLANNED, TravelStyleKeyword.FOODIE),
				result.travelStyleKeywords()
		);
		assertEquals("COMPLETED", result.onboardingStatus());

		CompanionProfile savedProfile = profileRepository.savedProfile;
		assertEquals(1L, savedProfile.userId());
		assertEquals("여행친구", savedProfile.nickname());
		assertEquals(UserGender.FEMALE, savedProfile.gender());
		assertEquals(null, savedProfile.birthYear());
		assertEquals(new BigDecimal("0.00"), savedProfile.mannerScore());
		assertEquals(0, savedProfile.reviewCount());
		assertEquals(CompanionProfileStatus.ACTIVE, savedProfile.status());
		assertEquals(1L, completeOnboardingUseCase.userId);
		assertIterableEquals(
				List.of(TravelStyleKeyword.PLANNED, TravelStyleKeyword.FOODIE),
				styleRepository.savedStyles.stream().map(CompanionProfileStyle::keyword).toList()
		);
	}

	@Test
	void rejectsDuplicateNickname() {
		profileRepository.duplicateNickname = true;

		assertThrows(DuplicateNicknameException.class, () -> service.register(new RegisterCompanionProfileCommand(
				1L,
				"여행친구",
				UserGender.FEMALE,
				null,
				null,
				List.of(TravelStyleKeyword.FOODIE)
		)));

		assertFalse(completeOnboardingUseCase.called);
	}

	@Test
	void rejectsDuplicateUserProfile() {
		profileRepository.duplicateUser = true;

		assertThrows(DuplicateCompanionProfileException.class, () -> service.register(new RegisterCompanionProfileCommand(
				1L,
				"여행친구",
				UserGender.FEMALE,
				null,
				null,
				List.of(TravelStyleKeyword.FOODIE)
		)));

		assertFalse(completeOnboardingUseCase.called);
	}

	@Test
	void doesNotSaveProfileWhenPhoneVerificationIsRequired() {
		completeOnboardingUseCase.exception = new PhoneVerificationRequiredException();

		assertThrows(PhoneVerificationRequiredException.class, () -> service.register(new RegisterCompanionProfileCommand(
				1L,
				"여행친구",
				UserGender.FEMALE,
				null,
				null,
				List.of(TravelStyleKeyword.FOODIE)
		)));

		assertEquals(null, profileRepository.savedProfile);
		assertEquals(0, styleRepository.savedStyles.size());
	}

	static class FakeCompanionProfileRepository implements CompanionProfileRepository {

		private boolean duplicateNickname;
		private boolean duplicateUser;
		private CompanionProfile savedProfile;

		@Override
		public CompanionProfile save(final CompanionProfile model) {
			savedProfile = new CompanionProfile(
					5L,
					model.userId(),
					model.nickname(),
					model.gender(),
					model.birthYear(),
					model.profileImageUrl(),
					model.intro(),
					model.mannerScore(),
					model.reviewCount(),
					model.status()
			);
			return savedProfile;
		}

		@Override
		public Optional<CompanionProfile> findById(final Long id) {
			return Optional.empty();
		}

		@Override
		public List<CompanionProfile> findAllByUserIdIn(final List<Long> userIds) {
			return List.of();
		}

		@Override
		public boolean existsByNickname(final String nickname) {
			return duplicateNickname;
		}

		@Override
		public boolean existsByUserId(final Long userId) {
			return duplicateUser;
		}

		@Override
		public Optional<CompanionProfile> findByUserId(final Long userId) {
			return Optional.empty();
		}
	}

	static class FakeCompanionProfileStyleRepository implements CompanionProfileStyleRepository {

		private final List<CompanionProfileStyle> savedStyles = new ArrayList<>();

		@Override
		public CompanionProfileStyle save(final CompanionProfileStyle model) {
			savedStyles.add(model);
			return model;
		}

		@Override
		public Optional<CompanionProfileStyle> findById(final CompanionProfileStyle.Key key) {
			return Optional.empty();
		}
	}

	static class FakeCompleteCompanionProfileOnboardingUseCase implements CompleteCompanionProfileOnboardingUseCase {

		private boolean called;
		private Long userId;
		private RuntimeException exception;

		@Override
		public String complete(final Long userId) {
			called = true;
			this.userId = userId;
			if (exception != null) {
				throw exception;
			}
			return "COMPLETED";
		}
	}
}

