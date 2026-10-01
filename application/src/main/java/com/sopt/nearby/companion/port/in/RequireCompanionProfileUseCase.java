// 동행 기능에 접근할 수 있는 사용자인지 확인하는 유스케이스다.
package com.sopt.nearby.companion.port.in;

public interface RequireCompanionProfileUseCase {
    void requireProfile(Long userId);
}
