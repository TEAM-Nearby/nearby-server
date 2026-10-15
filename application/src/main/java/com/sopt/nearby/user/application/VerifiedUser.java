// 검증된 소셜 로그인 사용자 식별자를 담는 모델
package com.sopt.nearby.user.application;

public record VerifiedUser(
		String providerUserId
) {
}