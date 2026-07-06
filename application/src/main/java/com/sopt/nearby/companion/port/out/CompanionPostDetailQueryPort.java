// 동행 모집 글 상세 조회용 쿼리 포트를 정의한다.
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.post.CompanionPostDetail;
import java.util.Optional;

public interface CompanionPostDetailQueryPort {

    Optional<CompanionPostDetail> findByPostId(Long postId, Long userId);
}
