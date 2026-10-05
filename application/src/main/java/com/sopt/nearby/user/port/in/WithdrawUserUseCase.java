// 인증된 회원의 탈퇴 유스케이스를 정의하는 포트
package com.sopt.nearby.user.port.in;

import com.sopt.nearby.user.application.WithdrawUserCommand;
import com.sopt.nearby.user.application.WithdrawUserResult;

public interface WithdrawUserUseCase {

	WithdrawUserResult withdraw(WithdrawUserCommand command);
}
