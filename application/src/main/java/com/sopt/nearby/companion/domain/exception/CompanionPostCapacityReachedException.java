// 동행 모집글의 전체 정원에 도달했을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionPostCapacityReachedException extends ConflictException {

    public CompanionPostCapacityReachedException() {
        super(CompanionErrorCode.COMPANION_POST_CAPACITY_REACHED);
    }
}
