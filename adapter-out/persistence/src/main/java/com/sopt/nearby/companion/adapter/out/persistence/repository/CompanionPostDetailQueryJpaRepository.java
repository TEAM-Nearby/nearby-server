// 동행 모집 글 상세 조회 네이티브 쿼리를 실행한다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompanionPostDetailQueryJpaRepository extends Repository<CompanionPostEntity, Long> {

    @Query(value = """
            select
                post.id as postId,
                post.host_user_id as hostUserId,
                host_profile.id as hostProfileId,
                host_profile.nickname as hostNickname,
                host_profile.gender as hostGender,
                host_profile.birth_year as hostBirthYear,
                host_profile.profile_image_url as hostProfileImageUrl,
                host_profile.manner_score as hostMannerScore,
                host_user.phone_verified_at as hostPhoneVerifiedAt,
                place.google_place_id as googlePlaceId,
                place.name as placeName,
                place.address as placeAddress,
                place.latitude as latitude,
                place.longitude as longitude,
                upper(coalesce(place.category, 'OTHER')) as placeCategory,
                post.meeting_at as meetingAt,
                post.max_participants as maxParticipants,
                post.content as content,
                post.open_chat_url as openChatUrl,
                post.status as status,
                post.created_at as createdAt,
                post.meeting_time_type as meetingTimeType,
                post.exposure_expires_at as expiresAt,
                cast(1 + coalesce(accepted.accepted_count, 0) as integer) as participantCount,
                current_app.status as applicationStatus
            from companion_post post
            join companion_profile host_profile
                on host_profile.user_id = post.host_user_id
            join user_account host_user
                on host_user.id = post.host_user_id
            join place_cache place
                on place.id = post.place_id
            left join (
                select latest.post_id, count(*) as accepted_count
                from (
                    select
                        app.post_id,
                        app.applicant_user_id,
                        app.status,
                        row_number() over (
                            partition by app.post_id, app.applicant_user_id
                            order by app.created_at desc, app.id desc
                        ) as rn
                    from companion_application app
                ) latest
                where latest.rn = 1
                    and latest.status = 'ACCEPTED'
                group by latest.post_id
            ) accepted
                on accepted.post_id = post.id
            left join companion_application current_app
                on current_app.id = (
                    select app.id
                    from companion_application app
                    where app.post_id = post.id
                        and app.applicant_user_id = :userId
                    order by app.created_at desc, app.id desc
                    limit 1
                )
            where post.id = :postId
            """, nativeQuery = true)
    Optional<CompanionPostDetailProjection> findDetailByPostId(
            @Param("postId") Long postId,
            @Param("userId") Long userId
    );

    @Query("""
            select style.keyword
            from CompanionProfileStyleEntity style
            where style.profileId = :profileId
            order by style.keyword
            """)
    List<TravelStyleKeyword> findKeywordsByProfileId(@Param("profileId") Long profileId);
}
