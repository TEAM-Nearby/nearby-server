// 동행 신청 검토 화면 상세 조회 요청 값을 애플리케이션 계층으로 전달한다.
package com.sopt.nearby.companion.application;

public record ReadCompanionRequestReviewCommand(
        Long hostUserId,
        Long applicationId
) {
}
