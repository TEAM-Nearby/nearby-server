// 휴대폰 인증 문자 발송 유스케이스의 입력 포트를 정의하는 인터페이스
package com.sopt.nearby.user.port.in;

import com.sopt.nearby.user.application.SendPhoneVerificationCodeCommand;
import com.sopt.nearby.user.application.SendPhoneVerificationCodeResult;

public interface SendPhoneVerificationCodeUseCase {

	SendPhoneVerificationCodeResult send(SendPhoneVerificationCodeCommand command);
}
