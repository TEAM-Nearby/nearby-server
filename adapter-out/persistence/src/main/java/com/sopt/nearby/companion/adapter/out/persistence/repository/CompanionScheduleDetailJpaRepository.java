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
                    when post.meeting_time_type = 'NOW' then post_place.google_place_id
                    else null
                end as googlePlaceId,
                case
                    when schedule.id is not null then schedule_place.name
                    when post.meeting_time_type = 'NOW' then post_place.name
                    else null
                end as placeName,
                case
                    when schedule.id is not null then schedule_place.address
                    when post.meeting_time_type = 'NOW' then post_place.address
                    else null
                end as placeAddress,
                case
                    when schedule.id is not null then schedule_place.latitude
                    when post.meeting_time_type = 'NOW' then post_place.latitude
                    else null
                end as latitude,
                case
                    when schedule.id is not null then schedule_place.longitude
                    when post.meeting_time_type = 'NOW' then post_place.longitude
                    else null
                end as longitude,
                schedule.scheduled_at as scheduledAt,
                case
                    when schedule.id is not null or post.meeting_time_type = 'NOW' then post.open_chat_url
                    else null
                end as openChatUrl,
                current_profile.nickname as userNickname,
                post.meeting_time_type as meetingTimeType
            from companion_match m
            join companion_post post
                on post.id = m.post_id
            left join companion_profile current_profile
                on current_profile.user_id = :userId
            left join companion_schedule schedule
                on schedule.match_id = m.id
                and schedule.confirmed = true
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
