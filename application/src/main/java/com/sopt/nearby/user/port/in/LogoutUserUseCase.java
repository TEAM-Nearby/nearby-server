// 사용자 로그아웃 유스케이스의 인바운드 포트를 정의하는 인터페이스
package com.sopt.nearby.user.port.in;

import com.sopt.nearby.user.application.LogoutUserCommand;
import com.sopt.nearby.user.application.LogoutUserResult;

public interface LogoutUserUseCase {

	LogoutUserResult logout(LogoutUserCommand command);
}
