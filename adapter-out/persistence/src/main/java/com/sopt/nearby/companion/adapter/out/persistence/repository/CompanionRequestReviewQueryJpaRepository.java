// 동행 신청 검토 화면 상세 조회 네이티브 쿼리를 실행한다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompanionRequestReviewQueryJpaRepository extends Repository<CompanionApplicationEntity, Long> {

    @Query(value = """
            select
                app.id as applicationId,
                app.post_id as postId,
                app.status as applicationStatus,
                post.host_user_id as hostUserId,
                place.name as placeName,
                place.address as placeAddress,
                post.meeting_time_type as meetingTimeType,
                coalesce((select schedule.scheduled_at
                    from companion_match match_row
                    join companion_schedule schedule on schedule.match_id = match_row.id and schedule.confirmed = true
                    where match_row.post_id = post.id and match_row.status = 'SCHEDULE_CONFIRMED'
                    order by match_row.created_at desc, match_row.id desc limit 1), post.meeting_at,
                    case when post.meeting_time_type = 'NOW' then app.created_at end) as meetingAt,
                post.exposure_expires_at as exposureExpiresAt,
                post.open_chat_url as openChatUrl,
                applicant_profile.id as applicantProfileId,
                applicant_profile.profile_image_url as applicantProfileImageUrl,
                applicant_profile.nickname as applicantNickname,
                applicant_profile.gender as applicantGender,
                applicant_profile.birth_year as applicantBirthYear,
                applicant_profile.manner_score as applicantMannerScore,
                applicant_user.phone_verified_at as applicantPhoneVerifiedAt
            from companion_application app
            join companion_post post
                on post.id = app.post_id
            join place_cache place
                on place.id = coalesce((select schedule.place_id
                    from companion_match match_row
                    join companion_schedule schedule on schedule.match_id = match_row.id and schedule.confirmed = true
                    where match_row.post_id = post.id and match_row.status = 'SCHEDULE_CONFIRMED'
                    order by match_row.created_at desc, match_row.id desc limit 1), post.place_id)
            join companion_profile applicant_profile
                on applicant_profile.user_id = app.applicant_user_id
            join user_account applicant_user
                on applicant_user.id = app.applicant_user_id
            where app.id = :applicationId
            """, nativeQuery = true)
    Optional<CompanionRequestReviewProjection> findReviewByApplicationId(
            @Param("applicationId") Long applicationId
    );
}
