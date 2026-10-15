// 비상 연락망 입력이 올바르지 않을 때 발생하는 예외다.
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class InvalidEmergencyContactException extends BusinessException {
    public InvalidEmergencyContactException() {
        super(OnboardingErrorCode.INVALID_EMERGENCY_CONTACT);
    }
}
