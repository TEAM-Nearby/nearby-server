// 동행 신청 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionApplicationJpaRepository extends JpaRepository<CompanionApplicationEntity, Long> {

	boolean existsByPostIdAndApplicantUserId(Long postId, Long applicantUserId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update CompanionApplicationEntity application
			set application.status = :status,
				application.rejectionReason = :rejectionReason
			where application.id = :applicationId
				and application.status = :currentStatus
			""")
	int updateStatusIfCurrentStatus(
			@Param("applicationId") Long applicationId,
			@Param("status") CompanionApplicationStatus status,
			@Param("rejectionReason") String rejectionReason,
			@Param("currentStatus") CompanionApplicationStatus currentStatus
	);
}
