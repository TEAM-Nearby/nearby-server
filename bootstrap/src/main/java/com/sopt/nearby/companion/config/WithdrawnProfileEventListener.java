// 회원 탈퇴 이벤트를 동행 프로필 익명화 서비스로 전달하는 Spring 리스너
package com.sopt.nearby.companion.config;

import com.sopt.nearby.companion.application.AnonymizeWithdrawnUserProfileListener;
import com.sopt.nearby.user.port.in.UserWithdrawnEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WithdrawnProfileEventListener {
	private final AnonymizeWithdrawnUserProfileListener service;

	public WithdrawnProfileEventListener(final AnonymizeWithdrawnUserProfileListener service) {
		this.service = service;
	}

	@EventListener
	public void onUserWithdrawn(final UserWithdrawnEvent event) {
		service.anonymize(event);
	}
}
