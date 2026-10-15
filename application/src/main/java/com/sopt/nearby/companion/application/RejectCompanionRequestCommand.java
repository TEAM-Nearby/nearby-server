// 동행 신청 거절 요청 값을 애플리케이션 계층으로 전달한다.
package com.sopt.nearby.companion.application;

public record RejectCompanionRequestCommand(
        Long hostUserId,
        Long applicationId,
        String rejectionReason
) {
}
