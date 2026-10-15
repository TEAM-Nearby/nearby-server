// 동행 신고 사유가 올바르지 않을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidCompanionReportReasonException extends BusinessException {

	public InvalidCompanionReportReasonException() {
		super(CompanionErrorCode.INVALID_COMPANION_REPORT_REASON);
	}
}
