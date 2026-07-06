// 동행 모집 글 작성 유스케이스의 진입 포트를 정의한다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.CreateCompanionPostCommand;
import com.sopt.nearby.companion.application.CreateCompanionPostResult;

public interface CreateCompanionPostUseCase {

    CreateCompanionPostResult create(CreateCompanionPostCommand command);
}
