// 동행 신청 결과 조회에 필요한 요청자와 신청 식별자를 전달하는 명령
package com.sopt.nearby.companion.application;

public record ReadCompanionRequestResultCommand(
        Long requesterUserId,
        Long applicationId
) {
}
