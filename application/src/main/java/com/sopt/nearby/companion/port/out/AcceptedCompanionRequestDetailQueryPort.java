// 수락된 동행 신청 결과 상세를 조회하는 포트
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.match.AcceptedCompanionRequestDetail;
import java.util.Optional;

public interface AcceptedCompanionRequestDetailQueryPort {

    Optional<AcceptedCompanionRequestDetail> findByApplicationIdAndRequesterUserId(
            Long applicationId,
            Long requesterUserId
    );
}
