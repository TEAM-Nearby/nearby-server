// 동행 참여자가 아닌 사용자의 신고를 막는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class ForbiddenCompanionReportException extends BusinessException {

	public ForbiddenCompanionReportException() {
		super(CompanionErrorCode.FORBIDDEN_COMPANION_REPORT);
	}
}
