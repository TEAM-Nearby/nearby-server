// 동행 매칭 게시글을 찾지 못했을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionPostNotFoundException extends NotFoundException {

    public CompanionPostNotFoundException() {
        super(CompanionErrorCode.COMPANION_MATCH_POST_NOT_FOUND);
    }
}
