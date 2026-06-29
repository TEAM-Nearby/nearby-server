// 긴급 연락처 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.adapter.persistence.user.repository;

import com.sopt.nearby.adapter.persistence.user.entity.EmergencyContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyContactJpaRepository extends JpaRepository<EmergencyContactEntity, Long> {
}
