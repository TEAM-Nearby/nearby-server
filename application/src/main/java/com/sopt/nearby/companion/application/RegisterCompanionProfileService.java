// 동행 프로필을 등록하고 회원 온보딩을 완료 처리하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.DuplicateCompanionProfileException;
import com.sopt.nearby.companion.domain.exception.DuplicateNicknameException;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStyle;
import com.sopt.nearby.companion.port.in.RegisterCompanionProfileUseCase;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileStyleRepository;
import com.sopt.nearby.user.port.in.CompleteCompanionProfileOnboardingUseCase;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;

public class RegisterCompanionProfileService implements RegisterCompanionProfileUseCase {

	private static final BigDecimal INITIAL_MANNER_SCORE = new BigDecimal("0.00");

	private final CompanionProfileRepository companionProfileRepository;
	private final CompanionProfileStyleRepository companionProfileStyleRepository;
	private final CompleteCompanionProfileOnboardingUseCase completeOnboardingUseCase;

	public RegisterCompanionProfileService(
			final CompanionProfileRepository companionProfileRepository,
			final CompanionProfileStyleRepository companionProfileStyleRepository,
			final CompleteCompanionProfileOnboardingUseCase completeOnboardingUseCase
	) {
		this.companionProfileRepository = companionProfileRepository;
		this.companionProfileStyleRepository = companionProfileStyleRepository;
		this.completeOnboardingUseCase = completeOnboardingUseCase;
	}

	@Override
	@Transactional
	public RegisteredCompanionProfileResult register(final RegisterCompanionProfileCommand command) {
		if (companionProfileRepository.existsByNickname(command.nickname())) {
			throw new DuplicateNicknameException();
		}
		if (companionProfileRepository.existsByUserId(command.userId())) {
			throw new DuplicateCompanionProfileException();
		}

		String onboardingStatus = completeOnboardingUseCase.complete(command.userId());
		CompanionProfile profile = companionProfileRepository.save(new CompanionProfile(
				null,
				command.userId(),
				command.nickname(),
				command.gender(),
				null,
				command.profileImageUrl(),
				command.intro(),
				INITIAL_MANNER_SCORE,
				0,
				CompanionProfileStatus.ACTIVE
		));
		command.travelStyleKeywords()
				.forEach(keyword -> companionProfileStyleRepository.save(
						new CompanionProfileStyle(profile.id(), keyword)
				));

		return new RegisteredCompanionProfileResult(
				profile.id(),
				profile.nickname(),
				profile.gender(),
				profile.intro(),
				profile.profileImageUrl(),
				command.travelStyleKeywords(),
				onboardingStatus
		);
	}
}

