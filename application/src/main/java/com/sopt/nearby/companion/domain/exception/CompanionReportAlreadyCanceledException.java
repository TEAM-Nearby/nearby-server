// 취소된 매칭 또는 만남에 대한 신고를 차단하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionReportAlreadyCanceledException extends BusinessException {

    public CompanionReportAlreadyCanceledException() {
        super(CompanionErrorCode.COMPANION_REPORT_ALREADY_CANCELED);
    }
}
