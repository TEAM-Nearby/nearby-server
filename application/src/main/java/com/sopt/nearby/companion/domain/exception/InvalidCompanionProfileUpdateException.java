// 동행 프로필 수정 입력이 유효하지 않을 때 발생하는 예외다.
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidCompanionProfileUpdateException extends BusinessException {
    public InvalidCompanionProfileUpdateException() {
        super(CompanionErrorCode.INVALID_COMPANION_PROFILE_UPDATE);
    }
}
