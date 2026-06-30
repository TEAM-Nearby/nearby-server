// 동행 매칭 참여자 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.adapter.persistence.companion.repository;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionMatchParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionMatchParticipantJpaRepository extends JpaRepository<CompanionMatchParticipantEntity, Long> {
}
