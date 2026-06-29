// 휴대폰 인증 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.adapter.persistence.user.repository;

import com.sopt.nearby.adapter.persistence.user.entity.PhoneVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneVerificationJpaRepository extends JpaRepository<PhoneVerificationEntity, Long> {
}
