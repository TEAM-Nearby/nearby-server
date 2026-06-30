// 회원의 약관 동의 이력을 표현하는 도메인 모델
package com.sopt.nearby.domain.user.model;

import java.time.LocalDateTime;

public record UserTermAgreement(
		Long id,
		Long userId,
		Long termId,
		LocalDateTime agreedAt
) {
}
