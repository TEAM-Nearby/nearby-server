// 미팅 체크인 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCheckInEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingCheckInJpaRepository extends JpaRepository<MeetingCheckInEntity, Long> {

    Optional<MeetingCheckInEntity> findByMeetingIdAndUserId(Long meetingId, Long userId);

    long countByMeetingId(Long meetingId);

    long countByMeetingIdAndCompletedAtIsNotNull(Long meetingId);
}
