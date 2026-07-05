// 동행 프로필 등록 이후 회원 온보딩 상태를 완료로 갱신하는 서비스
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.exception.PhoneVerificationRequiredException;
import com.sopt.nearby.user.exception.UserNotFoundException;
import com.sopt.nearby.user.port.in.CompleteCompanionProfileOnboardingUseCase;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompleteCompanionProfileOnboardingService implements CompleteCompanionProfileOnboardingUseCase {

	private final UserAccountRepository userAccountRepository;

	public CompleteCompanionProfileOnboardingService(final UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Override
	@Transactional
	public String complete(final Long userId) {
		UserAccount userAccount = userAccountRepository.findById(userId)
				.orElseThrow(UserNotFoundException::new);
		if (userAccount.onboardingStatus() != UserOnboardingStatus.PHONE_VERIFIED) {
			throw new PhoneVerificationRequiredException();
		}

		userAccountRepository.save(new UserAccount(
				userAccount.id(),
				userAccount.role(),
				userAccount.status(),
				userAccount.phoneNumber(),
				userAccount.phoneVerifiedAt(),
				UserOnboardingStatus.COMPLETED,
				userAccount.createdAt(),
				userAccount.deletedAt()
		));
		return UserOnboardingStatus.COMPLETED.name();
	}
}

