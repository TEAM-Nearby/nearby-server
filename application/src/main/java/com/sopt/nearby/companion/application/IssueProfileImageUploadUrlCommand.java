// 프로필 이미지 업로드 URL 발급 요청 값을 담는 커맨드
package com.sopt.nearby.companion.application;

public record IssueProfileImageUploadUrlCommand(
		Long userId,
		String fileName,
		String contentType,
		long fileSize
) {
}

