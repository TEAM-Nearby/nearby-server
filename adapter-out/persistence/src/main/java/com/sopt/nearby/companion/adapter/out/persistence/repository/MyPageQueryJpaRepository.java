// 마이페이지 조회 네이티브 쿼리를 실행한다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface MyPageQueryJpaRepository extends Repository<CompanionProfileEntity, Long> {

    @Query(value = """
            select
                profile.id as profileId,
                profile.user_id as userId,
                profile.nickname as nickname,
                profile.gender as gender,
                profile.birth_year as birthYear,
                profile.profile_image_url as profileImageUrl,
                profile.manner_score as mannerScore,
                profile.review_count as reviewCount,
                account.phone_verified_at as phoneVerifiedAt
            from companion_profile profile
            join user_account account
                on account.id = profile.user_id
            where profile.user_id = :userId
                and profile.status = 'ACTIVE'
            """, nativeQuery = true)
    Optional<MyPageProfileProjection> findProfileByUserId(@Param("userId") Long userId);

    @Query("""
            select style.keyword
            from CompanionProfileStyleEntity style
            where style.profileId = :profileId
            order by style.keyword
            """)
    List<TravelStyleKeyword> findTravelStyleKeywordsByProfileId(@Param("profileId") Long profileId);

    @Query(value = """
            select distinct keyword.keyword
            from companion_review review
            join companion_review_keyword keyword
                on keyword.review_id = review.id
            where review.reviewee_user_id = :userId
            order by keyword.keyword
            """, nativeQuery = true)
    List<String> findReceivedReviewKeywordsByUserId(@Param("userId") Long userId);

    @Query(value = """
            select distinct
                meeting.id as meetingId,
                place.name as placeName,
                place.address as placeAddress
            from companion_meeting meeting
            join companion_match_participant participant
                on participant.match_id = meeting.match_id
                and participant.user_id = :userId
            join companion_match cm
                on cm.id = meeting.match_id
            join companion_post post
                on post.id = cm.post_id
            left join companion_schedule schedule
                on schedule.match_id = cm.id
                and schedule.confirmed = true
            join place_cache place
                on place.id = coalesce(schedule.place_id, post.place_id)
            where meeting.status = 'COMPLETED'
            order by meeting.id asc
            """, nativeQuery = true)
    List<MyPageVisitedPlaceProjection> findCompletedMeetingPlacesByUserId(@Param("userId") Long userId);
}
