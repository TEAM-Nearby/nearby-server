// 동행 신고 요청이 올바르지 않을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidCompanionReportRequestException extends BusinessException {

	public InvalidCompanionReportRequestException() {
		super(CompanionErrorCode.INVALID_COMPANION_REPORT_REQUEST);
	}
}
