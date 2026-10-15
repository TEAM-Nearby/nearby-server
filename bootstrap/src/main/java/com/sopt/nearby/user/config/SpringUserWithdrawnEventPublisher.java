// 회원 탈퇴 이벤트 발행 포트를 Spring 이벤트로 연결하는 어댑터
package com.sopt.nearby.user.config;

import com.sopt.nearby.user.port.in.UserWithdrawnEvent;
import com.sopt.nearby.user.port.out.UserWithdrawnEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringUserWithdrawnEventPublisher implements UserWithdrawnEventPublisher {
	private final ApplicationEventPublisher publisher;

	public SpringUserWithdrawnEventPublisher(final ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void publish(final UserWithdrawnEvent event) {
		publisher.publishEvent(event);
	}
}
