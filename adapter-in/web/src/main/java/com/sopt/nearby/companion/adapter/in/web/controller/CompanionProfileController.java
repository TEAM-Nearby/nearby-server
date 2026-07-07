// 동행 프로필 상세 조회 HTTP 요청을 유스케이스로 전달한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionProfileResponse;
import com.sopt.nearby.companion.application.ReadCompanionProfileCommand;
import com.sopt.nearby.companion.port.in.ReadCompanionProfileUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companion-profiles")
public class CompanionProfileController implements CompanionProfileApi {

    private final ReadCompanionProfileUseCase readCompanionProfileUseCase;

    public CompanionProfileController(final ReadCompanionProfileUseCase readCompanionProfileUseCase) {
        this.readCompanionProfileUseCase = readCompanionProfileUseCase;
    }

    @Override
    @GetMapping("/{profileId}")
    public CommonResponse<CompanionProfileResponse> getProfile(
            @PathVariable final Long profileId,
            final Principal principal
    ) {
        return CommonResponse.success(
                CompanionSuccessCode.COMPANION_PROFILE_FOUND,
                CompanionProfileResponse.from(readCompanionProfileUseCase.read(
                        new ReadCompanionProfileCommand(Long.valueOf(principal.getName()), profileId)
                ))
        );
    }
}
