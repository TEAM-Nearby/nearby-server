// 회원 계정 HTTP 요청을 유스케이스로 전달하는 컨트롤러
package com.sopt.nearby.user.adapter.in.web.controller;

import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.user.adapter.in.web.dto.response.WithdrawUserResponse;
import com.sopt.nearby.user.adapter.in.web.response.UserAccountSuccessCode;
import com.sopt.nearby.user.application.WithdrawUserCommand;
import com.sopt.nearby.user.port.in.WithdrawUserUseCase;
import java.security.Principal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserAccountController implements UserAccountApi {

	private final WithdrawUserUseCase withdrawUserUseCase;

	public UserAccountController(final WithdrawUserUseCase withdrawUserUseCase) {
		this.withdrawUserUseCase = withdrawUserUseCase;
	}

	@Override
	@DeleteMapping("/me")
	public CommonResponse<WithdrawUserResponse> withdraw(final Principal principal) {
		return CommonResponse.success(
				UserAccountSuccessCode.WITHDRAW_USER,
				WithdrawUserResponse.from(withdrawUserUseCase.withdraw(
						new WithdrawUserCommand(Long.valueOf(principal.getName()))
				))
		);
	}
}
