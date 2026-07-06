// 동행 모집 글이 마감되어 조회할 수 없을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionPostExpiredException extends BusinessException {

    public CompanionPostExpiredException() {
        super(CompanionErrorCode.COMPANION_POST_EXPIRED);
    }
}
