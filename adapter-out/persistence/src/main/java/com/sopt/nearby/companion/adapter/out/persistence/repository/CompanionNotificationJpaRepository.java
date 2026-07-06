// 동행 알림 목록 조회 쿼리를 실행하는 JPA 저장소
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompanionNotificationJpaRepository extends Repository<CompanionApplicationEntity, Long> {

    @Query(value = """
            select
                app.id as applicationId,
                app.status as applicationStatus,
                host_profile.user_id as hostUserId,
                host_profile.profile_image_url as hostProfileImageUrl,
                host_profile.nickname as hostNickname,
                place.name as placeName,
                post.meeting_at as meetingAt,
                participant.match_id as matchId,
                case
                    when read_status.id is null then false
                    else true
                end as readStatus
            from companion_application app
            join companion_post post
                on post.id = app.post_id
            join companion_profile host_profile
                on host_profile.user_id = post.host_user_id
            left join place_cache place
                on place.id = post.place_id
            left join companion_match_participant participant
                on participant.accepted_application_id = app.id
            left join companion_application_read_status read_status
                on read_status.application_id = app.id
                and read_status.user_id = :userId
            where app.applicant_user_id = :userId
            order by app.created_at desc
            """, nativeQuery = true)
    List<CompanionNotificationProjection> findSentByUserId(@Param("userId") Long userId);

    @Query(value = """
            select
                app.id as applicationId,
                app.status as applicationStatus,
                host_profile.user_id as hostUserId,
                host_profile.profile_image_url as hostProfileImageUrl,
                host_profile.nickname as hostNickname,
                place.name as placeName,
                post.meeting_at as meetingAt,
                participant.match_id as matchId,
                case
                    when read_status.id is null then false
                    else true
                end as readStatus
            from companion_application app
            join companion_post post
                on post.id = app.post_id
            join companion_profile host_profile
                on host_profile.user_id = post.host_user_id
            left join place_cache place
                on place.id = post.place_id
            left join companion_match_participant participant
                on participant.accepted_application_id = app.id
            left join companion_application_read_status read_status
                on read_status.application_id = app.id
                and read_status.user_id = :userId
            where post.host_user_id = :userId
            order by app.created_at desc
            """, nativeQuery = true)
    List<CompanionNotificationProjection> findReceivedByUserId(@Param("userId") Long userId);
}

