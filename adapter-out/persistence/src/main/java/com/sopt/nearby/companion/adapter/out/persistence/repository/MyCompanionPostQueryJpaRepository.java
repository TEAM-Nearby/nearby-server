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
				schedule.scheduled_at as scheduledAt,
				place.google_place_id as googlePlaceId,
				place.name as placeName,
				place.address as placeAddress,
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
			left join (
				select latest_match.post_id, latest_match.id as match_id
				from (
					select
						match.id,
						match.post_id,
						row_number() over (
							partition by match.post_id
							order by match.created_at desc, match.id desc
						) as rn
					from companion_match match
					where match.status <> 'CANCELED'
				) latest_match
				where latest_match.rn = 1
			) selected_match
				on selected_match.post_id = post.id
			left join companion_schedule schedule
				on schedule.match_id = selected_match.match_id
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
				schedule.scheduled_at,
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
