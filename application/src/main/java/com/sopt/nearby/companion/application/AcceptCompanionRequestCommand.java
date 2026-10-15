// 동행 신청 수락 요청 값을 애플리케이션 계층으로 전달한다.
package com.sopt.nearby.companion.application;

public record AcceptCompanionRequestCommand(
        Long hostUserId,
        Long applicationId
) {
}
