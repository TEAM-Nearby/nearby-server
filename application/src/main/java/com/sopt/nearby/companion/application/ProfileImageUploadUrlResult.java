// 프로필 이미지 업로드 URL 발급 결과를 담는 응답 모델
package com.sopt.nearby.companion.application;

import java.util.Map;

public record ProfileImageUploadUrlResult(
		String uploadUrl,
		String imageUrl,
		String method,
		int expiresIn,
		Map<String, String> headers
) {
}

