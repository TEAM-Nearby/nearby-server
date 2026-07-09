// 동행 알림 목록 조회 쿼리를 실행하는 JPA 저장소
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionNotificationEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompanionNotificationQueryJpaRepository extends Repository<CompanionNotificationEntity, Long> {

    @Query(value = """
            select
                notification.id as notificationId,
                app.id as applicationId,
                app.status as applicationStatus,
                host_profile.user_id as hostUserId,
                host_profile.profile_image_url as hostProfileImageUrl,
                host_profile.nickname as hostNickname,
                place.name as placeName,
                coalesce(schedule.scheduled_at, post.meeting_at) as meetingAt,
                participant.match_id as matchId,
                case
                    when notification.read_at is null then false
                    else true
                end as readStatus
            from companion_notification notification
            join companion_application app
                on app.id = notification.target_id
                and notification.target_type = 'COMPANION_APPLICATION'
            join companion_post post
                on post.id = app.post_id
            join companion_profile host_profile
                on host_profile.user_id = post.host_user_id
            left join companion_match_participant participant
                on participant.accepted_application_id = app.id
            left join companion_schedule schedule
                on schedule.match_id = participant.match_id
                and schedule.confirmed = true
            left join place_cache place
                on place.id = coalesce(schedule.place_id, post.place_id)
            where notification.recipient_user_id = :userId
                and app.applicant_user_id = :userId
                and notification.notification_type in (
                    'COMPANION_APPLICATION_ACCEPTED',
                    'COMPANION_APPLICATION_REJECTED'
                )
            order by notification.created_at desc
            """, nativeQuery = true)
    List<CompanionNotificationProjection> findSentByUserId(@Param("userId") Long userId);

    @Query(value = """
            select
                notification.id as notificationId,
                app.id as applicationId,
                app.status as applicationStatus,
                host_profile.user_id as hostUserId,
                host_profile.profile_image_url as hostProfileImageUrl,
                host_profile.nickname as hostNickname,
                place.name as placeName,
                coalesce(schedule.scheduled_at, post.meeting_at) as meetingAt,
                participant.match_id as matchId,
                case
                    when notification.read_at is null then false
                    else true
                end as readStatus
            from companion_notification notification
            join companion_application app
                on app.id = notification.target_id
                and notification.target_type = 'COMPANION_APPLICATION'
            join companion_post post
                on post.id = app.post_id
            join companion_profile host_profile
                on host_profile.user_id = post.host_user_id
            left join companion_match_participant participant
                on participant.accepted_application_id = app.id
            left join companion_schedule schedule
                on schedule.match_id = participant.match_id
                and schedule.confirmed = true
            left join place_cache place
                on place.id = coalesce(schedule.place_id, post.place_id)
            where notification.recipient_user_id = :userId
                and post.host_user_id = :userId
                and notification.notification_type = 'COMPANION_APPLICATION_CREATED'
            order by notification.created_at desc
            """, nativeQuery = true)
    List<CompanionNotificationProjection> findReceivedByUserId(@Param("userId") Long userId);
}
