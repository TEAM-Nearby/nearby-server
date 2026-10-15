// 회원 탈퇴 중 공급자별 연동 해제 진행 상태를 담는 모델
package com.sopt.nearby.user.domain.model;

public record WithdrawalProgress(Long userId, String provider, State state) {
	public enum State {
		PENDING, IN_FLIGHT, SUCCEEDED, UNKNOWN
	}
}
