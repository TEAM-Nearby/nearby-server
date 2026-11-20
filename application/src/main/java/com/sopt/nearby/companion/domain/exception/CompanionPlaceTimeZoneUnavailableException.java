// 저장에 필요한 장소 시간대를 확인할 수 없을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionPlaceTimeZoneUnavailableException extends BusinessException {

    public CompanionPlaceTimeZoneUnavailableException() {
        super(CompanionErrorCode.COMPANION_PLACE_TIME_ZONE_UNAVAILABLE);
    }
}
