// 동행 상세 조회 쿼리를 실행하는 JPA 저장소
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompanionMeetingDetailQueryJpaRepository extends Repository<CompanionMeetingEntity, Long> {

    @Query(value = """
            select
                meeting.id as meetingId,
                current_participant.role as currentUserRole,
                host_profile.user_id as hostId,
                host_profile.gender as hostGender,
                host_profile.profile_image_url as hostProfileImageUrl,
                host_profile.nickname as hostNickname,
                case
                    when host_check_in.id is null then false
                    else true
                end as hostCheckedIn,
                place.name as placeName,
                schedule.scheduled_at as meetingAt,
                meeting.status as meetingStatus,
                case
                    when current_check_in.id is null then false
                    else true
                end as currentUserCheckedIn
            from companion_meeting meeting
            join companion_match_participant host_participant
                on host_participant.match_id = meeting.match_id
                and host_participant.role = 'HOST'
            join companion_profile host_profile
                on host_profile.user_id = host_participant.user_id
            join companion_schedule schedule
                on schedule.match_id = meeting.match_id
                and schedule.confirmed = true
            left join place_cache place
                on place.id = schedule.place_id
            left join companion_match_participant current_participant
                on current_participant.match_id = meeting.match_id
                and current_participant.user_id = :userId
            left join meeting_check_in host_check_in
                on host_check_in.meeting_id = meeting.id
                and host_check_in.user_id = host_participant.user_id
            left join meeting_check_in current_check_in
                on current_check_in.meeting_id = meeting.id
                and current_check_in.user_id = :userId
            where meeting.id = :meetingId
            """, nativeQuery = true)
    Optional<CompanionMeetingDetailProjection> findByMeetingIdAndUserId(
            @Param("meetingId") Long meetingId,
            @Param("userId") Long userId
    );
}
