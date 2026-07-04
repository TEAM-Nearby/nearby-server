// 휴대폰 인증 문자를 외부 발송 시스템으로 보내는 포트
package com.sopt.nearby.user.port.out;

public interface PhoneVerificationSender {

	void send(String phoneNumber, String verificationCode);
}
