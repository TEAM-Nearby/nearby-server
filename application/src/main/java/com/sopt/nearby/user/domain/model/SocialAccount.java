// 회원의 소셜 로그인 계정을 표현하는 도메인 모델
package com.sopt.nearby.user.domain.model;

public record SocialAccount(
		Long id,
		Long userId,
		String provider,
		String providerUserId
) {
}
