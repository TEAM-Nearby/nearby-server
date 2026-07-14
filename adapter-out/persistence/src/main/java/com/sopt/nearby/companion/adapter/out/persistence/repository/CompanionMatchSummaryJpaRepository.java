// 매칭된 동행 목록 조회 쿼리를 실행하는 JPA 저장소
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompanionMatchSummaryJpaRepository extends Repository<CompanionMatchEntity, Long> {

    @Query(value = """
            select
                m.id as matchId,
                host_profile.nickname as hostNickname,
                host_profile.profile_image_url as hostProfileImageUrl,
                host_profile.gender as hostGender,
                place.name as placeName,
                coalesce(
                    schedule.scheduled_at,
                    case
                        when post.meeting_time_type = 'NOW' then post.exposure_expires_at
                        else post.meeting_at
                    end
                ) as meetingAt,
                post.meeting_time_type as meetingTimeType,
                post.created_at as createdAt,
                post.content as content,
                m.status as matchStatus
            from companion_match m
            join companion_match_participant participant
                on participant.match_id = m.id
            join companion_post post
                on post.id = m.post_id
            join companion_profile host_profile
                on host_profile.user_id = post.host_user_id
            left join companion_schedule schedule
                on schedule.match_id = m.id
                and schedule.confirmed = true
            left join place_cache place
                on place.id = coalesce(schedule.place_id, post.place_id)
            where participant.user_id = :userId
                and m.status in ('MATCHED', 'SCHEDULE_CONFIRMED')
            order by m.created_at desc
            """, nativeQuery = true)
    List<CompanionMatchSummaryProjection> findAllByParticipantUserId(@Param("userId") Long userId);

    @Query(value = """
            select place.name
            from place_cache place
            where place.id = :placeId
            """, nativeQuery = true)
    Optional<String> findPlaceNameByPlaceId(@Param("placeId") Long placeId);
}
