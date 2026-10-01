// 동행 이용에 필요한 활성 프로필이 없을 때 발생하는 예외다.
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionProfileRequiredException extends BusinessException {
    public CompanionProfileRequiredException() {
        super(CompanionErrorCode.COMPANION_PROFILE_REQUIRED);
    }
}
