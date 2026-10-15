// 카카오 로그인 유스케이스의 입력 포트를 정의하는 인터페이스
package com.sopt.nearby.user.port.in;

import com.sopt.nearby.user.application.KakaoLoginCommand;
import com.sopt.nearby.user.application.KakaoLoginResult;

public interface KakaoLoginUseCase {

	KakaoLoginResult login(KakaoLoginCommand command);
}
