// 동행 신청 생성 요청 값을 애플리케이션 계층으로 전달하는 명령 객체
package com.sopt.nearby.companion.application;

public record CreateCompanionRequestCommand(
        Long applicantUserId,
        Long postId
) {
}
