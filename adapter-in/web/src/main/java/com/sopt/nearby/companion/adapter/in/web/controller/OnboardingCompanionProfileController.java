// 동행 프로필 온보딩 HTTP 요청을 유스케이스로 전달하는 컨트롤러
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.request.IssueProfileImageUploadUrlRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.request.RegisterCompanionProfileRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.ProfileImageUploadUrlResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.RegisteredCompanionProfileResponse;
import com.sopt.nearby.companion.application.ProfileImageUploadUrlResult;
import com.sopt.nearby.companion.application.RegisteredCompanionProfileResult;
import com.sopt.nearby.companion.port.in.IssueProfileImageUploadUrlUseCase;
import com.sopt.nearby.companion.port.in.RegisterCompanionProfileUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingCompanionProfileController implements OnboardingCompanionProfileApi {

	private final IssueProfileImageUploadUrlUseCase issueProfileImageUploadUrlUseCase;
	private final RegisterCompanionProfileUseCase registerCompanionProfileUseCase;

	public OnboardingCompanionProfileController(
			final IssueProfileImageUploadUrlUseCase issueProfileImageUploadUrlUseCase,
			final RegisterCompanionProfileUseCase registerCompanionProfileUseCase
	) {
		this.issueProfileImageUploadUrlUseCase = issueProfileImageUploadUrlUseCase;
		this.registerCompanionProfileUseCase = registerCompanionProfileUseCase;
	}

	@Override
	@PostMapping("/profile-images/presigned-url")
	public CommonResponse<ProfileImageUploadUrlResponse> issueProfileImageUploadUrl(
			@Valid @RequestBody final IssueProfileImageUploadUrlRequest request,
			@AuthenticationPrincipal final Jwt jwt
	) {
		ProfileImageUploadUrlResult result = issueProfileImageUploadUrlUseCase.issue(
				request.toCommand(Long.valueOf(jwt.getSubject()))
		);
		return CommonResponse.success(
				CompanionSuccessCode.PROFILE_IMAGE_UPLOAD_URL_ISSUED,
				ProfileImageUploadUrlResponse.from(result)
		);
	}

	@Override
	@PostMapping("/companion-profiles")
	public CommonResponse<RegisteredCompanionProfileResponse> registerCompanionProfile(
			@Valid @RequestBody final RegisterCompanionProfileRequest request,
			@AuthenticationPrincipal final Jwt jwt
	) {
		RegisteredCompanionProfileResult result = registerCompanionProfileUseCase.register(
				request.toCommand(Long.valueOf(jwt.getSubject()))
		);
		return CommonResponse.success(
				CompanionSuccessCode.COMPANION_PROFILE_CREATED,
				RegisteredCompanionProfileResponse.from(result)
		);
	}
}

