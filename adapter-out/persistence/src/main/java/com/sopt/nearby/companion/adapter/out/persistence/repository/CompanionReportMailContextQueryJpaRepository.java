// 동행 신고 이메일에 필요한 정보를 조회하는 JPA 저장소
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompanionReportMailContextQueryJpaRepository extends Repository<CompanionMatchEntity, Long> {

	@Query(value = """
			select
				reporter_profile.nickname as reporterNickname,
				reporter_account.phone_number as reporterPhoneNumber,
				reported_profile.nickname as reportedNickname,
				reported_account.phone_number as reportedPhoneNumber,
				post.id as companionPostId,
				post.content as companionPostContent,
				coalesce(schedule.scheduled_at, post.meeting_at) as scheduledAt,
				case when schedule.id is not null and post.meeting_time_type <> 'NOW'
						then 'SCHEDULED' else post.meeting_time_type end as meetingTimeType,
				place.address as placeAddress
			from companion_match m
			join companion_post post
				on post.id = m.post_id
			left join companion_schedule schedule
				on schedule.match_id = m.id
				and schedule.confirmed = true
			left join place_cache place
				on place.id = coalesce(schedule.place_id, post.place_id)
			join companion_profile reporter_profile
				on reporter_profile.user_id = :reporterUserId
			left join user_account reporter_account
				on reporter_account.id = :reporterUserId
			join companion_profile reported_profile
				on reported_profile.user_id = :reportedUserId
			left join user_account reported_account
				on reported_account.id = :reportedUserId
			where m.id = :matchId
			""", nativeQuery = true)
	Optional<CompanionReportMailContextProjection> findByMatchIdAndUserIds(
			@Param("matchId") Long matchId,
			@Param("reporterUserId") Long reporterUserId,
			@Param("reportedUserId") Long reportedUserId
	);
}
