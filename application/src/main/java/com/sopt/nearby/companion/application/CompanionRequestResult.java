// 동행 신청 상태와 수락된 경우의 상세를 전달하는 조회 결과
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.match.AcceptedCompanionRequestDetail;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;

public record CompanionRequestResult(
        Long applicationId,
        CompanionApplicationStatus applicationStatus,
        AcceptedCompanionRequestDetail acceptedDetail
) {
}
