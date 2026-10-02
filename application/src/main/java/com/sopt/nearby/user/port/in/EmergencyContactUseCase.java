// 인증된 사용자의 비상 연락망 한 건을 저장하고 조회하는 유스케이스다.
package com.sopt.nearby.user.port.in;

import com.sopt.nearby.user.application.SaveEmergencyContactCommand;
import com.sopt.nearby.user.domain.model.EmergencyContact;
import java.util.Optional;

public interface EmergencyContactUseCase {
    EmergencyContact save(SaveEmergencyContactCommand command);
    Optional<EmergencyContact> read(Long userId);
}
