// 수락된 동행 신청 상세를 조회하는 JPA 저장소
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AcceptedCompanionRequestDetailJpaRepository
        extends Repository<CompanionApplicationEntity, Long> {

    @Query(value = """
            select
                companion_match.id as matchId,
                companion_match.status as matchStatus,
                post.id as postId,
                host_profile.user_id as hostUserId,
                host_profile.nickname as hostNickname,
                host_profile.profile_image_url as hostProfileImageUrl,
                case
                    when confirmed_schedule.id is not null then schedule_place.google_place_id
                    else post_place.google_place_id
                end as googlePlaceId,
                case
                    when confirmed_schedule.id is not null then schedule_place.name
                    else post_place.name
                end as placeName,
                case
                    when confirmed_schedule.id is not null then schedule_place.address
                    else post_place.address
                end as placeAddress,
                case
                    when confirmed_schedule.id is not null then schedule_place.latitude
                    else post_place.latitude
                end as placeLatitude,
                case
                    when confirmed_schedule.id is not null then schedule_place.longitude
                    else post_place.longitude
                end as placeLongitude,
                post.meeting_time_type as meetingTimeType,
                case
                    when confirmed_schedule.id is not null then confirmed_schedule.scheduled_at
                    when post.meeting_time_type = 'SCHEDULED' then post.meeting_at
                    when post.meeting_time_type = 'NOW' then post.exposure_expires_at
                    else null
                end as meetingAt,
                (
                    select count(*)
                    from companion_match_participant participant_count
                    where participant_count.match_id = companion_match.id
                ) as participantCount,
                post.max_participants as maxParticipants,
                post.open_chat_url as openChatUrl
            from companion_application application
            join companion_match_participant guest_participant
                on guest_participant.accepted_application_id = application.id
                and guest_participant.user_id = application.applicant_user_id
                and guest_participant.role = 'GUEST'
            join companion_match
                on companion_match.id = guest_participant.match_id
            join companion_post post
                on post.id = companion_match.post_id
                and post.id = application.post_id
            join companion_profile host_profile
                on host_profile.user_id = post.host_user_id
            join place_cache post_place
                on post_place.id = post.place_id
            left join companion_schedule confirmed_schedule
                on confirmed_schedule.match_id = companion_match.id
                and confirmed_schedule.confirmed = true
            left join place_cache schedule_place
                on schedule_place.id = confirmed_schedule.place_id
            where application.id = :applicationId
                and application.applicant_user_id = :requesterUserId
                and application.status = 'ACCEPTED'
            order by guest_participant.id desc
            limit 1
            """, nativeQuery = true)
    Optional<AcceptedCompanionRequestDetailProjection> findByApplicationIdAndRequesterUserId(
            @Param("applicationId") Long applicationId,
            @Param("requesterUserId") Long requesterUserId
    );
}
