// 회원 계정의 활성 상태 조회 유스케이스를 정의하는 포트
package com.sopt.nearby.user.port.in;

public interface CheckActiveUserUseCase {

	boolean isActive(Long userId);
}
