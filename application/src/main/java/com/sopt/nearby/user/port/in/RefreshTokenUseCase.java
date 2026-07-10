// 리프레시 토큰으로 토큰을 재발급하는 인바운드 포트
package com.sopt.nearby.user.port.in;

import com.sopt.nearby.user.application.RefreshTokenCommand;
import com.sopt.nearby.user.application.RefreshTokenResult;

public interface RefreshTokenUseCase {

	RefreshTokenResult refresh(RefreshTokenCommand command);
}
