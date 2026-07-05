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
                schedule.id as scheduleId,
                place.google_place_id as googlePlaceId,
                place.name as placeName,
                place.address as placeAddress,
                place.latitude as latitude,
                place.longitude as longitude,
                schedule.scheduled_at as scheduledAt,
                case
                    when schedule.id is null then null
                    else post.open_chat_url
                end as openChatUrl
            from companion_match m
            join companion_post post
                on post.id = m.post_id
            left join companion_schedule schedule
                on schedule.match_id = m.id
                and schedule.confirmed = true
            left join place_cache place
                on place.id = schedule.place_id
            where m.id = :matchId
            """, nativeQuery = true)
    Optional<CompanionScheduleDetailProjection> findByMatchId(@Param("matchId") Long matchId);
}