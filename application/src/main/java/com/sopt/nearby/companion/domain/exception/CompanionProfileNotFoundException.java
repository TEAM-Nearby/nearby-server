//동행 프로필을 찾지 못 했을 때 생기는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionProfileNotFoundException extends NotFoundException {
    public CompanionProfileNotFoundException(){
        super(CompanionErrorCode.COMPANION_PROFILE_NOT_FOUND);
    }
}
