// 검증된 카카오 ID 토큰에서 얻은 사용자 식별자를 담는 모델
package com.sopt.nearby.user.application;

public record VerifiedKakaoUser(
		String providerUserId
) {
}
