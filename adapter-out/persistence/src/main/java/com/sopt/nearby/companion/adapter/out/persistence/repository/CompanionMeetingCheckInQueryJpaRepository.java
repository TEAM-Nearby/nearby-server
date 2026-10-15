// 만남 인증에 필요한 만남, 일정, 장소 정보를 조회하는 JPA 저장소
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.projection.CompanionMeetingCheckInProjection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionMeetingCheckInQueryJpaRepository extends JpaRepository<CompanionMeetingEntity, Long> {

    @Query(value = """
            select meeting.id as meetingId,
                   meeting.match_id as matchId,
                   meeting.status as meetingStatus,
                   schedule.id as scheduleId,
                   schedule.place_id as placeId,
                   schedule.scheduled_at as scheduledAt,
                   place.latitude as placeLatitude,
                   place.longitude as placeLongitude
            from companion_meeting meeting
            left join companion_schedule schedule
                   on schedule.match_id = meeting.match_id
                  and schedule.confirmed = true
            left join place_cache place
                   on place.id = schedule.place_id
            where meeting.id = :meetingId
            """, nativeQuery = true)
    Optional<CompanionMeetingCheckInProjection> findContextByMeetingId(@Param("meetingId") Long meetingId);
}
