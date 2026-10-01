// 회원 온보딩 진행 상태를 정의하는 enum
package com.sopt.nearby.user.domain.model;

public enum UserOnboardingStatus {
	STARTED,
	TERMS_AGREED,
	PHONE_VERIFIED,
	COMPANION_PROFILE_COMPLETED,
	COMPANION_PROFILE_SKIPPED,
	COMPLETED;

	public boolean isCompleted() {
		return this == COMPLETED || this == COMPANION_PROFILE_COMPLETED || this == COMPANION_PROFILE_SKIPPED;
	}

	public boolean hasCompanionProfile() {
		return this == COMPLETED || this == COMPANION_PROFILE_COMPLETED;
	}

	public String apiStatus() {
		if (isCompleted()) {
			return COMPLETED.name();
		}
		return this == PHONE_VERIFIED ? PHONE_VERIFIED.name() : STARTED.name();
	}
}
