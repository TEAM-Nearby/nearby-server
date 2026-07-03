// 이미 등록된 소셜 계정 저장 충돌을 표현하는 예외
package com.sopt.nearby.user.exception;

public class SocialAccountAlreadyExistsException extends RuntimeException {

	public SocialAccountAlreadyExistsException() {
		super("이미 등록된 소셜 계정입니다.");
	}

	public SocialAccountAlreadyExistsException(final Throwable cause) {
		super("이미 등록된 소셜 계정입니다.", cause);
	}
}
