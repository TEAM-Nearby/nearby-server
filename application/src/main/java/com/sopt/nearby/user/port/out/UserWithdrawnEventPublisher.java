// 회원 탈퇴 완료 이벤트 발행을 추상화하는 포트
package com.sopt.nearby.user.port.out;

import com.sopt.nearby.user.port.in.UserWithdrawnEvent;

public interface UserWithdrawnEventPublisher {
	void publish(UserWithdrawnEvent event);
}
