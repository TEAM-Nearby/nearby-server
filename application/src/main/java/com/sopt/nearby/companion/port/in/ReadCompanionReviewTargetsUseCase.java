// 동행 후기 대상 목록 조회 유스케이스 진입점을 정의하는 포트
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.ReadCompanionReviewTargetsResult;

public interface ReadCompanionReviewTargetsUseCase {

	ReadCompanionReviewTargetsResult getTargets(Long meetingId, Long userId);
}
