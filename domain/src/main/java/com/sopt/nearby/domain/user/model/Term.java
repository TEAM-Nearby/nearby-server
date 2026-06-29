// 서비스 약관 항목을 표현하는 도메인 모델
package com.sopt.nearby.domain.user.model;

public record Term(
		Long id,
		String termKey,
		String version,
		boolean required
) {
}
