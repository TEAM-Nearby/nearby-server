// 주변 동행 모집글 목록 조회 쿼리 포트를 정의한다.
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.application.ReadNearbyCompanionPostsCommand;
import com.sopt.nearby.companion.domain.model.post.NearbyCompanionPostSummary;
import java.util.List;

public interface NearbyCompanionPostQueryPort {

    List<NearbyCompanionPostSummary> findNearby(ReadNearbyCompanionPostsCommand command);
}
