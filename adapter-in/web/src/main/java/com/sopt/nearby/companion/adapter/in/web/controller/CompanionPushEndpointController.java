// 동행 푸시 수신 대상 등록과 해제 HTTP 요청을 처리한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.request.RegisterCompanionPushEndpointRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionPushEndpointResponse;
import com.sopt.nearby.companion.application.DeactivateCompanionPushEndpointCommand;
import com.sopt.nearby.companion.port.in.DeactivateCompanionPushEndpointUseCase;
import com.sopt.nearby.companion.port.in.RegisterCompanionPushEndpointUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/push-endpoints")
public class CompanionPushEndpointController implements CompanionPushEndpointApi {

    private final RegisterCompanionPushEndpointUseCase registerUseCase;
    private final DeactivateCompanionPushEndpointUseCase deactivateUseCase;

    public CompanionPushEndpointController(
            final RegisterCompanionPushEndpointUseCase registerUseCase,
            final DeactivateCompanionPushEndpointUseCase deactivateUseCase
    ) {
        this.registerUseCase = registerUseCase;
        this.deactivateUseCase = deactivateUseCase;
    }

    @Override
    @PostMapping
    public CommonResponse<CompanionPushEndpointResponse> register(
            @RequestBody final RegisterCompanionPushEndpointRequest request,
            final Principal principal
    ) {
        return CommonResponse.success(
                CompanionSuccessCode.REGISTER_COMPANION_PUSH_ENDPOINT,
                CompanionPushEndpointResponse.from(registerUseCase.register(
                        request.toCommand(Long.valueOf(principal.getName()))
                ))
        );
    }

    @Override
    @DeleteMapping("/{installationId}")
    public CommonResponse<Void> deactivate(
            @PathVariable final String installationId,
            final Principal principal
    ) {
        deactivateUseCase.deactivate(new DeactivateCompanionPushEndpointCommand(
                Long.valueOf(principal.getName()),
                installationId
        ));
        return CommonResponse.success(CompanionSuccessCode.DEACTIVATE_COMPANION_PUSH_ENDPOINT);
    }
}
