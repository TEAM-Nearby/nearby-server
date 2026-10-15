// 동행 후기 대상 목록 조회 쿼리를 실행하는 JPA 저장소
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompanionReviewTargetQueryJpaRepository extends Repository<CompanionMeetingEntity, Long> {

	@Query(value = """
			select
				target_profile.user_id as revieweeUserId,
				target_profile.profile_image_url as profileImageUrl,
				target_profile.nickname as nickname,
				coalesce(nullif(place.address, ''), place.name, '') as placeAddress,
				schedule.scheduled_at as meetingAt,
				true as checkedIn,
				case
					when review.id is null then false
					else true
				end as hasWrittenReview
			from companion_meeting meeting
			join companion_match_participant target_participant
				on target_participant.match_id = meeting.match_id
				and target_participant.role = :targetRole
			join companion_profile target_profile
				on target_profile.user_id = target_participant.user_id
			join meeting_check_in target_check_in
				on target_check_in.meeting_id = meeting.id
				and target_check_in.user_id = target_participant.user_id
			join companion_schedule schedule
				on schedule.match_id = meeting.match_id
				and schedule.confirmed = true
			left join place_cache place
				on place.id = schedule.place_id
			left join companion_review review
				on review.meeting_id = meeting.id
				and review.reviewer_user_id = :reviewerUserId
				and review.reviewee_user_id = target_participant.user_id
			where meeting.id = :meetingId
			order by target_participant.id asc
			""", nativeQuery = true)
	List<CompanionReviewTargetProjection> findAllByMeetingIdAndReviewerUserIdAndTargetRole(
			@Param("meetingId") Long meetingId,
			@Param("reviewerUserId") Long reviewerUserId,
			@Param("targetRole") String targetRole
	);
}
