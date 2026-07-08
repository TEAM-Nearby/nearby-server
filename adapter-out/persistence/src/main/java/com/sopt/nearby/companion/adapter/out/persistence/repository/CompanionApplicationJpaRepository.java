// 동행 신청 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionApplicationJpaRepository extends JpaRepository<CompanionApplicationEntity, Long> {

	boolean existsByPostIdAndApplicantUserId(Long postId, Long applicantUserId);
}
