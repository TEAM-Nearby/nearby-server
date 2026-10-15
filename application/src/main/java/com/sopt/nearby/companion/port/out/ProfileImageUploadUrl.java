// 스토리지 어댑터가 발급한 프로필 이미지 업로드 URL 정보
package com.sopt.nearby.companion.port.out;

import java.util.Map;

public record ProfileImageUploadUrl(
		String uploadUrl,
		String imageUrl,
		String method,
		int expiresIn,
		Map<String, String> headers
) {
}

