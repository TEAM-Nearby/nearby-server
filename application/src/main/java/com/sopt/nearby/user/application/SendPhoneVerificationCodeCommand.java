// 휴대폰 인증 문자 발송 요청에 필요한 사용자와 전화번호를 담는 커맨드
package com.sopt.nearby.user.application;

public record SendPhoneVerificationCodeCommand(
		Long userId,
		String phoneNumber
) {
}
