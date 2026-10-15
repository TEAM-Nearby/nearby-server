// 회원 온보딩 완료 여부 확인 유스케이스를 정의한다.
package com.sopt.nearby.user.port.in;

public interface RequireCompletedOnboardingUseCase {

    void requireCompleted(Long userId);
}
