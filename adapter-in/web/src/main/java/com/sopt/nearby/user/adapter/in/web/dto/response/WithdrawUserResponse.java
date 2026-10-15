// 회원 탈퇴 성공 여부를 반환하는 응답 DTO
package com.sopt.nearby.user.adapter.in.web.dto.response;

import com.sopt.nearby.user.application.WithdrawUserResult;

public record WithdrawUserResponse(boolean withdrawn) {

	public static WithdrawUserResponse from(final WithdrawUserResult result) {
		return new WithdrawUserResponse(result.withdrawn());
	}
}
