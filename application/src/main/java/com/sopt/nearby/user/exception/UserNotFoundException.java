// 온보딩 대상 사용자를 찾지 못한 경우를 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {

	public UserNotFoundException() {
		super(OnboardingErrorCode.USER_NOT_FOUND);
	}
}
