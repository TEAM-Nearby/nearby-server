// 내가 작성한 동행 모집글 목록 조회 네이티브 쿼리를 실행한다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface MyCompanionPostQueryJpaRepository extends Repository<CompanionPostEntity, Long> {

	@Query(value = """
			select
				post.id as postId,
				max(schedule.scheduled_at) as scheduledAt,
				place.google_place_id as googlePlaceId,
				place.name as placeName,
				coalesce(nullif(place.address, ''), place.name, '') as placeAddress,
				place.latitude as latitude,
				place.longitude as longitude,
				cast(1 + coalesce(accepted.accepted_count, 0) as integer) as currentParticipants,
				post.max_participants as maxParticipants,
				post.content as content
			from companion_post post
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
			left join companion_match match
				on match.post_id = post.id
				and match.status <> 'CANCELED'
			left join companion_schedule schedule
				on schedule.match_id = match.id
				and schedule.confirmed = true
			where post.host_user_id = :hostUserId
				and post.status <> 'CANCELED'
			group by
				post.id,
				place.id,
				place.google_place_id,
				place.name,
				place.address,
				place.latitude,
				place.longitude,
				accepted.accepted_count,
				post.max_participants,
				post.content,
				post.created_at
			order by post.created_at desc, post.id desc
			""", nativeQuery = true)
	List<MyCompanionPostProjection> findAllByHostUserId(@Param("hostUserId") Long hostUserId);

	@Query(value = """
			select distinct
				match.post_id as postId,
				keyword.keyword as keyword
			from companion_match match
			join companion_meeting meeting
				on meeting.match_id = match.id
			join companion_review review
				on review.meeting_id = meeting.id
				and review.reviewee_user_id = :hostUserId
			join companion_review_keyword keyword
				on keyword.review_id = review.id
			where match.post_id in (:postIds)
			order by match.post_id asc, keyword.keyword asc
			""", nativeQuery = true)
	List<MyCompanionPostKeywordProjection> findReviewKeywordsByPostIds(
			@Param("postIds") List<Long> postIds,
			@Param("hostUserId") Long hostUserId
	);
}
