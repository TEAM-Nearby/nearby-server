// 자기 자신에 대한 신고를 막는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionReportSelfNotAllowedException extends BusinessException {

	public CompanionReportSelfNotAllowedException() {
		super(CompanionErrorCode.COMPANION_REPORT_SELF_NOT_ALLOWED);
	}
}
