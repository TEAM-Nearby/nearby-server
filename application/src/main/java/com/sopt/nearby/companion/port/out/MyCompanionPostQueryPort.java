// 내가 작성한 동행 모집글 목록 조회 쿼리 포트를 정의한다.
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.post.MyCompanionPostSummary;
import java.util.List;

public interface MyCompanionPostQueryPort {

	List<MyCompanionPostSummary> findAllByHostUserId(Long hostUserId);
}
