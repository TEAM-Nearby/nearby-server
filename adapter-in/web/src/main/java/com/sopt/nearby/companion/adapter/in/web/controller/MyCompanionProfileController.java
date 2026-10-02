// 인증된 사용자의 동행 프로필 조회와 수정 요청을 전달한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.request.UpdateMyCompanionProfileRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.MyCompanionProfileResponse;
import com.sopt.nearby.companion.port.in.ReadMyCompanionProfileUseCase;
import com.sopt.nearby.companion.port.in.UpdateMyCompanionProfileUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/companion-profile")
public class MyCompanionProfileController implements MyCompanionProfileApi {
    private final ReadMyCompanionProfileUseCase readProfile;
    private final UpdateMyCompanionProfileUseCase updateProfile;

    public MyCompanionProfileController(final ReadMyCompanionProfileUseCase readProfile,
                                        final UpdateMyCompanionProfileUseCase updateProfile) {
        this.readProfile = readProfile;
        this.updateProfile = updateProfile;
    }

    @Override
    @GetMapping
    public CommonResponse<MyCompanionProfileResponse> getProfile(final Principal principal) {
        return CommonResponse.success(CompanionSuccessCode.MY_COMPANION_PROFILE_FOUND,
                MyCompanionProfileResponse.from(readProfile.read(Long.valueOf(principal.getName()))));
    }

    @Override
    @PutMapping
    public CommonResponse<MyCompanionProfileResponse> updateProfile(
            @Valid @RequestBody final UpdateMyCompanionProfileRequest request, final Principal principal) {
        return CommonResponse.success(CompanionSuccessCode.COMPANION_PROFILE_UPDATED,
                MyCompanionProfileResponse.from(updateProfile.update(request.toCommand(Long.valueOf(principal.getName())))));
    }
}
