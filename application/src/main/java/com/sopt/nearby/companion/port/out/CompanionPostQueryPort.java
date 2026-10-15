// 동행 모집글 목록 조회 쿼리 포트를 정의한다.
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.application.ReadCompanionPostsCommand;
import com.sopt.nearby.companion.domain.model.post.CompanionPostSummary;
import java.util.List;

public interface CompanionPostQueryPort {

    List<CompanionPostSummary> find(ReadCompanionPostsCommand command);
}
