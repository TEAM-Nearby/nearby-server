// 내 동행 일정 조회 쿼리를 실행하는 JPA 저장소
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompanionScheduleDetailJpaRepository extends Repository<CompanionMatchEntity, Long> {

    @Query(value = """
            select
                m.id as matchId,
                m.status as matchStatus,
                case
                    when schedule.id is not null then schedule_place.google_place_id
                    else post_place.google_place_id
                end as googlePlaceId,
                case
                    when schedule.id is not null then schedule_place.name
                    else post_place.name
                end as placeName,
                case
                    when schedule.id is not null then schedule_place.address
                    else post_place.address
                end as placeAddress,
                case
                    when schedule.id is not null then schedule_place.latitude
                    else post_place.latitude
                end as latitude,
                case
                    when schedule.id is not null then schedule_place.longitude
                    else post_place.longitude
                end as longitude,
                case
                    when schedule.id is not null then schedule.scheduled_at
                    when post.meeting_time_type = 'SCHEDULED' then post.meeting_at
                    when post.meeting_time_type = 'NOW' then post.exposure_expires_at
                    else null
                end as scheduledAt,
                post.open_chat_url as openChatUrl,
                current_profile.nickname as userNickname,
                post.meeting_time_type as meetingTimeType,
                current_participant.role as currentUserRole
            from companion_match m
            join companion_post post
                on post.id = m.post_id
            left join companion_match_participant current_participant
                on current_participant.match_id = m.id
                and current_participant.user_id = :userId
            left join companion_profile current_profile
                on current_profile.user_id = :userId
            left join companion_schedule schedule
                on schedule.match_id = m.id
                and schedule.confirmed = true
                and m.status = 'SCHEDULE_CONFIRMED'
            left join place_cache schedule_place
                on schedule_place.id = schedule.place_id
            left join place_cache post_place
                on post_place.id = post.place_id
            where m.id = :matchId
            """, nativeQuery = true)
    Optional<CompanionScheduleDetailProjection> findByMatchIdAndUserId(
            @Param("matchId") Long matchId,
            @Param("userId") Long userId
    );
}
