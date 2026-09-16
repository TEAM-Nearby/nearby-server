// 애플 로그인 유스케이스의 입력 포트를 정의하는 인터페이스
package com.sopt.nearby.user.port.in;

import com.sopt.nearby.user.application.AppleLoginCommand;
import com.sopt.nearby.user.application.AppleLoginResult;

public interface AppleLoginUseCase {

    AppleLoginResult login(AppleLoginCommand command);
}
