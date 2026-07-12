// 동행 미팅 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionMeetingJpaRepository extends JpaRepository<CompanionMeetingEntity, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select meeting from CompanionMeetingEntity meeting where meeting.id = :meetingId")
	Optional<CompanionMeetingEntity> findByIdForUpdate(@Param("meetingId") Long meetingId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			update companion_meeting
			set status = 'COMPLETED',
				completed_at = :completedAt
			where id = :meetingId
				and status = 'ONGOING'
			""", nativeQuery = true)
	int completeIfOngoing(
			@Param("meetingId") Long meetingId,
			@Param("completedAt") LocalDateTime completedAt
	);
}
