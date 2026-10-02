// 활성 상태가 아닌 동행 프로필의 조회와 수정을 차단한다.
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InactiveCompanionProfileException extends BusinessException {
    public InactiveCompanionProfileException() {
        super(CompanionErrorCode.FORBIDDEN_INACTIVE_COMPANION_PROFILE);
    }
}
