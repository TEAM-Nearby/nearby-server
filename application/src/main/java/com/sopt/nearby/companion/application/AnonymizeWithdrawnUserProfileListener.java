// 탈퇴한 회원의 동행 프로필 개인정보를 익명화하는 이벤트 리스너
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.user.port.in.UserWithdrawnEvent;
import org.springframework.stereotype.Service;

@Service
public class AnonymizeWithdrawnUserProfileListener {

	private static final String WITHDRAWN_NICKNAME_PREFIX = "탈퇴한 사용자-";

	private final CompanionProfileRepository companionProfileRepository;

	public AnonymizeWithdrawnUserProfileListener(final CompanionProfileRepository companionProfileRepository) {
		this.companionProfileRepository = companionProfileRepository;
	}

	public void anonymize(final UserWithdrawnEvent event) {
		companionProfileRepository.findByUserId(event.userId()).ifPresent(profile ->
				companionProfileRepository.save(new CompanionProfile(
						profile.id(),
						profile.userId(),
						WITHDRAWN_NICKNAME_PREFIX + profile.userId(),
						profile.gender(),
						null,
						null,
						null,
						profile.mannerScore(),
						profile.reviewCount(),
						CompanionProfileStatus.INACTIVE
				))
		);
	}
}
