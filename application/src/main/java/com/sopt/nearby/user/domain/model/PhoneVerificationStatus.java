// 휴대폰 인증 요청의 처리 상태를 정의하는 enum
package com.sopt.nearby.user.domain.model;

public enum PhoneVerificationStatus {
	PENDING,
	VERIFIED,
	EXPIRED,
	FAILED
}
