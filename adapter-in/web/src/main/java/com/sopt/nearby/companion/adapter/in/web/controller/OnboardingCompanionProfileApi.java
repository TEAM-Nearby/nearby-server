// 동행 프로필 온보딩 API의 Swagger 문서 계약을 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.request.IssueProfileImageUploadUrlRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.request.RegisterCompanionProfileRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.ProfileImageUploadUrlResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.RegisteredCompanionProfileResponse;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag(name = "Onboarding", description = "사용자 온보딩 API")
public interface OnboardingCompanionProfileApi {

	@Operation(
			summary = "프로필 이미지 업로드 URL 발급",
			description = "프로필 이미지 업로드에 사용할 S3 Presigned URL을 발급합니다.",
			security = @SecurityRequirement(name = "bearerAuth")
	)
	CommonResponse<ProfileImageUploadUrlResponse> issueProfileImageUploadUrl(
			IssueProfileImageUploadUrlRequest request,
			@Parameter(hidden = true) Jwt jwt
	);

	@Operation(
			summary = "동행 프로필 등록 및 온보딩 완료",
			description = "동행 프로필을 등록하고 사용자 온보딩 상태를 완료로 갱신합니다.",
			security = @SecurityRequirement(name = "bearerAuth")
	)
	CommonResponse<RegisteredCompanionProfileResponse> registerCompanionProfile(
			RegisterCompanionProfileRequest request,
			@Parameter(hidden = true) Jwt jwt
	);
}

