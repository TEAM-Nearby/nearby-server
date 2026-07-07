// 동행 프로필 상세 조회 요청 값을 담는 커맨드
package com.sopt.nearby.companion.application;

public record ReadCompanionProfileCommand(
        Long viewerUserId,
        Long profileId
) {
}
