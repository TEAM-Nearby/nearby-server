// 신고 보고서에 필요한 동행 정보를 찾지 못했을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionReportContextNotFoundException extends NotFoundException {

	public CompanionReportContextNotFoundException() {
		super(CompanionErrorCode.COMPANION_REPORT_CONTEXT_NOT_FOUND);
	}
}
