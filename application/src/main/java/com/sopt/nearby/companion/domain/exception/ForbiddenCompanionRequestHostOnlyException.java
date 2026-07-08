// 동행 신청을 호스트가 아닌 사용자가 조회할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class ForbiddenCompanionRequestHostOnlyException extends BusinessException {

    public ForbiddenCompanionRequestHostOnlyException() {
        super(CompanionErrorCode.FORBIDDEN_COMPANION_REQUEST_HOST_ONLY);
    }
}
