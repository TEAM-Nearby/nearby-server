// 동행 매칭 참여자 정보를 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model.match;

public record CompanionMatchParticipant(
		Long id,
		Long matchId,
		Long userId,
		Long acceptedApplicationId,
		MatchParticipantRole role
) {
}
