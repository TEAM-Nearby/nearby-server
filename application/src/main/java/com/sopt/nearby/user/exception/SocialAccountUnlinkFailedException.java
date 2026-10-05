// 소셜 제공자의 계정 연동 해제 실패를 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class SocialAccountUnlinkFailedException extends BusinessException {

	public SocialAccountUnlinkFailedException() {
		super(AccountWithdrawalErrorCode.SOCIAL_ACCOUNT_UNLINK_FAILED);
	}
}
