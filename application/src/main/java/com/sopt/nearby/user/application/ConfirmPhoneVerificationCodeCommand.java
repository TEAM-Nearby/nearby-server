// 휴대폰 인증 번호 확인 요청 값을 전달하는 command
package com.sopt.nearby.user.application;

public record ConfirmPhoneVerificationCodeCommand(
		Long userId,
		Long phoneVerificationId,
		String verificationCode
) {
}
