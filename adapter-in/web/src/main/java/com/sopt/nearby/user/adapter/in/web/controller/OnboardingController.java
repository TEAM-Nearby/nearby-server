// 온보딩 상태와 비상 연락망 HTTP 요청을 유스케이스에 전달한다.
package com.sopt.nearby.user.adapter.in.web.controller;

import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.user.adapter.in.web.dto.request.SaveEmergencyContactRequest;
import com.sopt.nearby.user.adapter.in.web.dto.response.EmergencyContactResponse;
import com.sopt.nearby.user.adapter.in.web.dto.response.OnboardingStatusResponse;
import com.sopt.nearby.user.adapter.in.web.response.OnboardingSuccessCode;
import com.sopt.nearby.user.port.in.EmergencyContactUseCase;
import com.sopt.nearby.user.port.in.ReadOnboardingStatusUseCase;
import com.sopt.nearby.user.port.in.SkipCompanionProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController implements OnboardingApi {
    private final ReadOnboardingStatusUseCase readStatus;
    private final SkipCompanionProfileUseCase skipProfile;
    private final EmergencyContactUseCase contacts;

    public OnboardingController(final ReadOnboardingStatusUseCase readStatus,
                                final SkipCompanionProfileUseCase skipProfile,
                                final EmergencyContactUseCase contacts) {
        this.readStatus = readStatus;
        this.skipProfile = skipProfile;
        this.contacts = contacts;
    }

    @Override
    @GetMapping
    public CommonResponse<OnboardingStatusResponse> getStatus(@AuthenticationPrincipal final Jwt jwt) {
        return CommonResponse.success(OnboardingSuccessCode.ONBOARDING_STATUS_FOUND,
                OnboardingStatusResponse.from(readStatus.read(Long.valueOf(jwt.getSubject()))));
    }

    @Override
    @PostMapping("/companion-profiles/skip")
    public CommonResponse<OnboardingStatusResponse> skipProfile(@AuthenticationPrincipal final Jwt jwt) {
        return CommonResponse.success(OnboardingSuccessCode.COMPANION_PROFILE_SKIP_PROCESSED,
                OnboardingStatusResponse.from(skipProfile.skip(Long.valueOf(jwt.getSubject()))));
    }

    @Override
    @PutMapping("/emergency-contact")
    public CommonResponse<EmergencyContactResponse> saveContact(
            @Valid @RequestBody final SaveEmergencyContactRequest request,
            @AuthenticationPrincipal final Jwt jwt) {
        return CommonResponse.success(OnboardingSuccessCode.EMERGENCY_CONTACT_SAVED,
                EmergencyContactResponse.from(contacts.save(request.toCommand(Long.valueOf(jwt.getSubject())))));
    }

    @Override
    @GetMapping("/emergency-contact")
    public CommonResponse<EmergencyContactResponse> getContact(@AuthenticationPrincipal final Jwt jwt) {
        return CommonResponse.success(OnboardingSuccessCode.EMERGENCY_CONTACT_FOUND,
                contacts.read(Long.valueOf(jwt.getSubject())).map(EmergencyContactResponse::from).orElse(null));
    }
}
