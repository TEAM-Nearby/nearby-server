// 진행 중인 동행 목록 조회 쿼리를 실행하는 JPA 저장소
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface OngoingCompanionMeetingQueryJpaRepository extends Repository<CompanionMeetingEntity, Long> {

    @Query(value = """
            select
                meeting.id as meetingId,
                m.id as matchId,
                host_profile.user_id as hostUserId,
                host_profile.profile_image_url as hostProfileImageUrl,
                host_profile.nickname as hostNickname,
                host_profile.gender as hostGender,
                place.name as placeName,
                schedule.scheduled_at as meetingAt,
                case
                    when m.status = 'MATCHED' then post.meeting_time_type
                    when post.meeting_time_type = 'NOW' then 'NOW'
                    else 'SCHEDULED'
                end as meetingTimeType,
                case
                    when check_in.id is null then false
                    else true
                end as checkedIn,
                meeting.status as meetingStatus,
                case
                    when m.status = 'MATCHED' then 'SCHEDULING'
                    else 'ONGOING'
                end as progressStatus
            from companion_match m
            join companion_match_participant current_participant
                on current_participant.match_id = m.id
                and current_participant.user_id = :userId
            join companion_post post
                on post.id = m.post_id
            join companion_match_participant host_participant
                on host_participant.match_id = m.id
                and host_participant.role = 'HOST'
            join companion_profile host_profile
                on host_profile.user_id = host_participant.user_id
            left join companion_schedule schedule
                on schedule.match_id = m.id
                and schedule.confirmed = true
            left join companion_meeting meeting
                on meeting.match_id = m.id
                and meeting.status = 'ONGOING'
            left join place_cache place
                on place.id = coalesce(schedule.place_id, post.place_id)
            left join meeting_check_in check_in
                on check_in.meeting_id = meeting.id
                and check_in.user_id = :userId
            where m.status = 'MATCHED'
                or meeting.id is not null
            order by
                case when m.status = 'MATCHED' then 1 else 0 end,
                schedule.scheduled_at desc,
                m.id desc
            """, nativeQuery = true)
    List<OngoingCompanionMeetingProjection> findAllByParticipantUserId(@Param("userId") Long userId);
}
