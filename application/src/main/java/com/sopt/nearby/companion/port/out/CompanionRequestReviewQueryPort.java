// 동행 신청 검토 화면 상세 조회용 쿼리 포트를 정의한다.
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.match.CompanionRequestReview;
import java.util.Optional;

public interface CompanionRequestReviewQueryPort {

    Optional<CompanionRequestReview> findByApplicationId(Long applicationId);
}
