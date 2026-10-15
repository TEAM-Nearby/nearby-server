// 동행 중이 아닌 미팅의 신고를 막는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionReportMeetingNotOngoingException extends BusinessException {

	public CompanionReportMeetingNotOngoingException() {
		super(CompanionErrorCode.COMPANION_REPORT_MEETING_NOT_ONGOING);
	}
}
