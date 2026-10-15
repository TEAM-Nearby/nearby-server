// 동행 모집글이 신청 가능한 모집 상태가 아닐 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionPostNotRecruitingException extends ConflictException {

    public CompanionPostNotRecruitingException() {
        super(CompanionErrorCode.COMPANION_POST_NOT_RECRUITING);
    }
}
