// 카카오, 애플 로그인 HTTP 요청을 유스케이스로 전달하는 컨트롤러
package com.sopt.nearby.user.adapter.in.web.controller;

import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.user.adapter.in.web.dto.request.AppleLoginRequest;
import com.sopt.nearby.user.adapter.in.web.dto.request.KakaoLoginRequest;
import com.sopt.nearby.user.adapter.in.web.dto.response.AppleLoginResponse;
import com.sopt.nearby.user.adapter.in.web.dto.response.KakaoLoginResponse;
import com.sopt.nearby.user.adapter.in.web.response.AppleLoginSuccessCode;
import com.sopt.nearby.user.adapter.in.web.response.KakaoLoginSuccessCode;
import com.sopt.nearby.user.port.in.AppleLoginUseCase;
import com.sopt.nearby.user.port.in.KakaoLoginUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/login")
public class LoginController implements KakaoLoginApi, AppleLoginApi {

    private final KakaoLoginUseCase kakaoLoginUseCase;
    private final AppleLoginUseCase appleLoginUseCase;

    public LoginController(
            final KakaoLoginUseCase kakaoLoginUseCase,
            final AppleLoginUseCase appleLoginUseCase
    ) {
        this.kakaoLoginUseCase = kakaoLoginUseCase;
        this.appleLoginUseCase = appleLoginUseCase;
    }

    @Override
    @PostMapping("/kakao")
    public CommonResponse<KakaoLoginResponse> login(@Valid @RequestBody final KakaoLoginRequest request) {
        return CommonResponse.success(
                KakaoLoginSuccessCode.KAKAO_LOGIN_SUCCESS,
                KakaoLoginResponse.from(kakaoLoginUseCase.login(request.toCommand()))
        );
    }

    @Override
    @PostMapping("/apple")
    public CommonResponse<AppleLoginResponse> appleLogin(
            @Valid @RequestBody final AppleLoginRequest request
    ) {
        return CommonResponse.success(
                AppleLoginSuccessCode.APPLE_LOGIN_SUCCESS,
                AppleLoginResponse.from(appleLoginUseCase.login(request.toCommand()))
        );
    }
}
