// 동행 모집 글 상세 조회 요청 값을 애플리케이션 계층으로 전달한다.
package com.sopt.nearby.companion.application;

public record ReadCompanionPostDetailCommand(
        Long userId,
        Long postId
) {
}
