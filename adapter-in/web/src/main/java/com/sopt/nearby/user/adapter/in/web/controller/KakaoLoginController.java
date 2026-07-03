// 카카오 로그인 HTTP 요청을 유스케이스로 전달하는 컨트롤러
package com.sopt.nearby.user.adapter.in.web.controller;

import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.user.adapter.in.web.dto.request.KakaoLoginRequest;
import com.sopt.nearby.user.adapter.in.web.dto.response.KakaoLoginResponse;
import com.sopt.nearby.user.adapter.in.web.response.KakaoLoginSuccessCode;
import com.sopt.nearby.user.port.in.KakaoLoginUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kakao")
public class KakaoLoginController implements KakaoLoginApi {

	private final KakaoLoginUseCase kakaoLoginUseCase;

	public KakaoLoginController(final KakaoLoginUseCase kakaoLoginUseCase) {
		this.kakaoLoginUseCase = kakaoLoginUseCase;
	}

	@Override
	@PostMapping("/login")
	public CommonResponse<KakaoLoginResponse> login(@Valid @RequestBody final KakaoLoginRequest request) {
		return CommonResponse.success(
				KakaoLoginSuccessCode.KAKAO_LOGIN_SUCCESS,
				KakaoLoginResponse.from(kakaoLoginUseCase.login(request.toCommand()))
		);
	}
}
