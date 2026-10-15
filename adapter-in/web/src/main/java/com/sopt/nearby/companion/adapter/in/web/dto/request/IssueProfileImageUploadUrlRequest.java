// 프로필 이미지 업로드 URL 발급 요청 본문을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.request;

import com.sopt.nearby.companion.application.IssueProfileImageUploadUrlCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record IssueProfileImageUploadUrlRequest(
		@Schema(description = "업로드할 원본 파일명", example = "profile.jpg")
		@NotBlank(message = "지원하지 않는 이미지 형식이거나 또는 파일 크기를 초과했습니다.")
		String fileName,

		@Schema(description = "이미지 MIME 타입", example = "image/jpeg")
		@NotBlank(message = "지원하지 않는 이미지 형식이거나 또는 파일 크기를 초과했습니다.")
		@Pattern(
				regexp = "image/(jpeg|png|webp)",
				message = "지원하지 않는 이미지 형식이거나 또는 파일 크기를 초과했습니다."
		)
		String contentType,

		@Schema(description = "파일 크기 byte", example = "524288")
		@NotNull(message = "지원하지 않는 이미지 형식이거나 또는 파일 크기를 초과했습니다.")
		@Positive(message = "지원하지 않는 이미지 형식이거나 또는 파일 크기를 초과했습니다.")
		@Max(value = 5_242_880L, message = "지원하지 않는 이미지 형식이거나 또는 파일 크기를 초과했습니다.")
		Long fileSize
) {

	public IssueProfileImageUploadUrlCommand toCommand(final Long userId) {
		return new IssueProfileImageUploadUrlCommand(userId, fileName, contentType, fileSize);
	}
}

