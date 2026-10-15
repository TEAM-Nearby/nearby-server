// 동행 프로필 등록 이후 회원 온보딩을 완료 처리하는 유스케이스 포트
package com.sopt.nearby.user.port.in;

public interface CompleteCompanionProfileOnboardingUseCase {

	String complete(Long userId);
}

