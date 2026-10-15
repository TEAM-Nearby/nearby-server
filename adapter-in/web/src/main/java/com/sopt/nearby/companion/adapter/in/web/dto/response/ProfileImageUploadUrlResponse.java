// 프로필 이미지 업로드 URL 발급 결과를 API 응답으로 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.ProfileImageUploadUrlResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public record ProfileImageUploadUrlResponse(
		@Schema(description = "S3 업로드용 Presigned URL")
		String uploadUrl,
		@Schema(description = "프로필 등록 시 사용할 이미지 URL")
		String imageUrl,
		@Schema(description = "S3 업로드 HTTP 메서드", example = "PUT")
		String method,
		@Schema(description = "업로드 URL 만료까지 남은 초", example = "300")
		int expiresIn,
		@Schema(description = "S3 업로드 시 함께 보낼 헤더")
		Map<String, String> headers
) {

	public static ProfileImageUploadUrlResponse from(final ProfileImageUploadUrlResult result) {
		return new ProfileImageUploadUrlResponse(
				result.uploadUrl(),
				result.imageUrl(),
				result.method(),
				result.expiresIn(),
				result.headers()
		);
	}
}

