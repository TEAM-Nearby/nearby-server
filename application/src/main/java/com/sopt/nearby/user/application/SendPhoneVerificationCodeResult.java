// 휴대폰 인증 문자 발송 결과로 인증 요청 식별자와 만료 시간을 반환하는 DTO
package com.sopt.nearby.user.application;

public record SendPhoneVerificationCodeResult(
		Long phoneVerificationId,
		int expiresIn
) {
}
