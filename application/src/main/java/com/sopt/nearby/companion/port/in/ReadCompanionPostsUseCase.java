// 동행 모집글 목록 조회 유스케이스를 정의한다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.CompanionPostsResult;
import com.sopt.nearby.companion.application.ReadCompanionPostsCommand;

public interface ReadCompanionPostsUseCase {

    CompanionPostsResult read(ReadCompanionPostsCommand command);
}
