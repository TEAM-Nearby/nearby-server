// 동행 신청 검토 화면 상세 조회 유스케이스를 정의한다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.CompanionRequestReviewResult;
import com.sopt.nearby.companion.application.ReadCompanionRequestReviewCommand;

public interface ReadCompanionRequestReviewUseCase {

    CompanionRequestReviewResult read(ReadCompanionRequestReviewCommand command);
}
