// 만남 인증을 완료하지 않은 사용자의 신고를 막는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionReportCurrentUserNotCheckedInException extends BusinessException {

	public CompanionReportCurrentUserNotCheckedInException() {
		super(CompanionErrorCode.COMPANION_REPORT_CURRENT_USER_NOT_CHECKED_IN);
	}
}
