// 동행 매칭 정보를 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model;

import java.time.LocalDateTime;

public record CompanionMatch(
		Long id,
		Long postId,
		CompanionMatchStatus status,
		LocalDateTime createdAt
) {
}
