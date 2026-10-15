// 회원 계정이 현재 활성 상태인지 조회하는 서비스
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.port.in.CheckActiveUserUseCase;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class CheckActiveUserService implements CheckActiveUserUseCase {

	private final UserAccountRepository userAccountRepository;

	public CheckActiveUserService(final UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Override
	public boolean isActive(final Long userId) {
		return userAccountRepository.findById(userId)
				.map(user -> user.status() == UserAccountStatus.ACTIVE)
				.orElse(false);
	}
}
