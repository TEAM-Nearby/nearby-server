// 긴급 연락처 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.user.adapter.out.persistence.repository;

import com.sopt.nearby.user.adapter.out.persistence.entity.EmergencyContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmergencyContactJpaRepository extends JpaRepository<EmergencyContactEntity, Long> {
	Optional<EmergencyContactEntity> findByUserId(Long userId);

	void deleteByUserId(Long userId);
}
