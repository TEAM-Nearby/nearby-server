// 동행 참여자가 아닌 신고 대상을 지정했을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionReportTargetNotFoundException extends NotFoundException {

	public CompanionReportTargetNotFoundException() {
		super(CompanionErrorCode.COMPANION_REPORT_TARGET_NOT_FOUND);
	}
}
