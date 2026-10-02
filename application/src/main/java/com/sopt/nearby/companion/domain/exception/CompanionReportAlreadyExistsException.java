// 같은 동행과 사용자를 다시 신고했을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionReportAlreadyExistsException extends ConflictException {

	public CompanionReportAlreadyExistsException() {
		super(CompanionErrorCode.COMPANION_REPORT_ALREADY_EXISTS);
	}
}
