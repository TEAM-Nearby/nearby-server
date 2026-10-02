// 온보딩 상태 조회와 선택 정보 등록 API의 문서를 정의한다.
package com.sopt.nearby.user.adapter.in.web.controller;

import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import com.sopt.nearby.user.adapter.in.web.dto.request.SaveEmergencyContactRequest;
import com.sopt.nearby.user.adapter.in.web.dto.response.EmergencyContactResponse;
import com.sopt.nearby.user.adapter.in.web.dto.response.OnboardingStatusResponse;
import com.sopt.nearby.user.exception.InvalidEmergencyContactException;
import com.sopt.nearby.user.exception.PhoneVerificationRequiredException;
import com.sopt.nearby.user.exception.UserNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag(name = "Onboarding", description = "사용자 온보딩 API")
@SecurityRequirement(name = "bearerAuth")
public interface OnboardingApi {
    @Operation(summary = "현재 온보딩 상태 조회",
            description = "앱 재실행 시 진행 단계와 프로필·비상 연락망 등록 여부를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "온보딩 상태 조회에 성공했습니다.")
    @ApiExceptions(UserNotFoundException.class)
    CommonResponse<OnboardingStatusResponse> getStatus(@Parameter(hidden = true) Jwt jwt);

    @Operation(summary = "동행 프로필 설정 건너뛰기",
            description = "휴대폰 인증 후 프로필 없이 온보딩을 완료합니다. 비상 연락망은 선택 사항입니다. "
                    + "반복 호출해도 상태를 유지하며, 이미 프로필을 등록한 사용자는 등록 완료 상태를 유지합니다. "
                    + "건너뛴 뒤에도 기존 프로필 등록 API를 사용할 수 있습니다.")
    @ApiResponse(responseCode = "200", description = "현재 온보딩 상태를 반환합니다.")
    @ApiExceptions({PhoneVerificationRequiredException.class, UserNotFoundException.class})
    CommonResponse<OnboardingStatusResponse> skipProfile(@Parameter(hidden = true) Jwt jwt);

    @Operation(summary = "비상 연락망 등록",
            description = "선택 사항인 비상 연락망 한 명을 저장합니다. 기존 연락망이 있으면 같은 항목을 갱신하며 "
                    + "반복 요청으로 추가 항목을 만들지 않습니다. 온보딩 상태는 변경하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "비상 연락망 저장에 성공했습니다.")
    @ApiExceptions({InvalidEmergencyContactException.class, UserNotFoundException.class})
    CommonResponse<EmergencyContactResponse> saveContact(SaveEmergencyContactRequest request,
                                                        @Parameter(hidden = true) Jwt jwt);

    @Operation(summary = "내 비상 연락망 조회",
            description = "인증된 사용자 본인의 연락망만 조회합니다. 미등록이면 data는 null입니다.")
    @ApiResponse(responseCode = "200", description = "비상 연락망 조회에 성공했습니다.")
    @ApiExceptions(UserNotFoundException.class)
    CommonResponse<EmergencyContactResponse> getContact(@Parameter(hidden = true) Jwt jwt);
}
