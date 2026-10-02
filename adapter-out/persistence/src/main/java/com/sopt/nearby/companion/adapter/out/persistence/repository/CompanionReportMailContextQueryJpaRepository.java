// 동행 신고 이메일에 필요한 정보를 조회하는 JPA 저장소
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompanionReportMailContextQueryJpaRepository extends Repository<CompanionMeetingEntity, Long> {

	@Query(value = """
			select
				reporter_profile.nickname as reporterNickname,
				reporter_account.phone_number as reporterPhoneNumber,
				reported_profile.nickname as reportedNickname,
				reported_account.phone_number as reportedPhoneNumber,
				post.id as companionPostId,
				post.content as companionPostContent,
				schedule.scheduled_at as scheduledAt,
				post.meeting_time_type as meetingTimeType,
				place.address as placeAddress
			from companion_meeting meeting
			join companion_match m
				on m.id = meeting.match_id
			join companion_post post
				on post.id = m.post_id
			join companion_schedule schedule
				on schedule.match_id = m.id
				and schedule.confirmed = true
			left join place_cache place
				on place.id = schedule.place_id
			join companion_profile reporter_profile
				on reporter_profile.user_id = :reporterUserId
			left join user_account reporter_account
				on reporter_account.id = :reporterUserId
			join companion_profile reported_profile
				on reported_profile.user_id = :reportedUserId
			left join user_account reported_account
				on reported_account.id = :reportedUserId
			where meeting.id = :meetingId
			""", nativeQuery = true)
	Optional<CompanionReportMailContextProjection> findByMeetingIdAndUserIds(
			@Param("meetingId") Long meetingId,
			@Param("reporterUserId") Long reporterUserId,
			@Param("reportedUserId") Long reportedUserId
	);
}
