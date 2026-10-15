// 긴급 연락처 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.user.port.out;

import com.sopt.nearby.common.port.DomainRepository;
import com.sopt.nearby.user.domain.model.EmergencyContact;
import java.util.Optional;

public interface EmergencyContactRepository extends DomainRepository<EmergencyContact, Long> {
	Optional<EmergencyContact> findByUserId(Long userId);

	void deleteByUserId(Long userId);
}
