// 주변 동행 모집글 목록 조회 유스케이스를 정의한다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.NearbyCompanionPostsResult;
import com.sopt.nearby.companion.application.ReadNearbyCompanionPostsCommand;

public interface ReadNearbyCompanionPostsUseCase {

    NearbyCompanionPostsResult read(ReadNearbyCompanionPostsCommand command);
}
