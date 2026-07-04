// 휴대폰 인증 번호 확인 유스케이스의 진입 포트를 정의하는 인터페이스
package com.sopt.nearby.user.port.in;

import com.sopt.nearby.user.application.ConfirmPhoneVerificationCodeCommand;
import com.sopt.nearby.user.application.ConfirmPhoneVerificationCodeResult;

public interface ConfirmPhoneVerificationCodeUseCase {

	ConfirmPhoneVerificationCodeResult confirm(ConfirmPhoneVerificationCodeCommand command);
}
