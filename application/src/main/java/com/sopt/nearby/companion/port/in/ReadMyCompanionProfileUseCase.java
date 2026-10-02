// 인증된 사용자 본인의 프로필 수정 화면 조회를 제공한다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.MyCompanionProfileResult;

public interface ReadMyCompanionProfileUseCase {
    MyCompanionProfileResult read(Long userId);
}
