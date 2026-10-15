// 비상 연락망 등록 입력을 정규화하고 검증한다.
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.exception.InvalidEmergencyContactException;

public record SaveEmergencyContactCommand(Long userId, String name, String phoneNumber) {
    public SaveEmergencyContactCommand {
        name = name == null ? null : name.strip();
        phoneNumber = phoneNumber == null ? null : phoneNumber.strip();
        if (userId == null || userId <= 0 || name == null || name.isBlank() || name.length() > 50
                || phoneNumber == null || !phoneNumber.matches("\\+?[0-9]{8,15}")) {
            throw new InvalidEmergencyContactException();
        }
    }
}
