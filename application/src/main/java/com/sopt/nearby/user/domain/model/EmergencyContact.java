// 회원의 긴급 연락처를 표현하는 도메인 모델
package com.sopt.nearby.user.domain.model;

public record EmergencyContact(
		Long id,
		Long userId,
		String name,
		String phoneNumber
) {
}
