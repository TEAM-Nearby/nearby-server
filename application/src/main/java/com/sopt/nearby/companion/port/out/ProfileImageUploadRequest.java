// 프로필 이미지 업로드 URL 발급에 필요한 스토리지 요청 모델
package com.sopt.nearby.companion.port.out;

public record ProfileImageUploadRequest(
		Long userId,
		String fileName,
		String contentType,
		long fileSize
) {
}

