// 동행 모집글 목록 조회 네이티브 쿼리를 실행한다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompanionPostQueryJpaRepository extends Repository<CompanionPostEntity, Long> {

    @Query(value = """
            with post_rows as (
                select
                    post.id as postId,
                    post.status as status,
                    host_profile.nickname as hostNickname,
                    host_profile.gender as hostGender,
                    place.id as placeId,
                    place.google_place_id as googlePlaceId,
                    place.name as placeName,
                    upper(coalesce(place.category, 'OTHER')) as placeCategory,
                    place.latitude as latitude,
                    place.longitude as longitude,
                    6371000 * acos(least(1.0, greatest(-1.0,
                        cos(radians(:latitude)) * cos(radians(cast(place.latitude as double precision)))
                        * cos(radians(cast(place.longitude as double precision)) - radians(:longitude))
                        + sin(radians(:latitude)) * sin(radians(cast(place.latitude as double precision)))
                    ))) as distanceMeters,
                    place.photo_reference as photoReference,
                    post.content as content,
                    post.meeting_at as meetingAt,
                    post.exposure_expires_at as exposureExpiresAt,
                    cast(1 + coalesce(accepted.accepted_count, 0) as integer) as participantCount,
                    post.max_participants as maxParticipants,
                    post.created_at as createdAt
                from companion_post post
                join companion_profile host_profile
                    on host_profile.user_id = post.host_user_id
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
                where post.status = 'RECRUITING'
                    and (
                        (post.meeting_time_type = 'NOW' and post.exposure_expires_at > current_timestamp)
                        or (post.meeting_time_type = 'SCHEDULED' and post.meeting_at > current_timestamp)
                        or post.meeting_time_type = 'UNDECIDED'
                    )
            )
            select
                postId,
                status,
                hostNickname,
                hostGender,
                placeId,
                googlePlaceId,
                placeName,
                placeCategory,
                latitude,
                longitude,
                cast(round(distanceMeters) as integer) as distanceMeters,
                photoReference,
                content,
                meetingAt,
                participantCount,
                maxParticipants,
                createdAt
            from post_rows
            where distanceMeters <= :radiusMeters
                and (:placeCategory = 'ALL' or placeCategory = :placeCategory)
                and (:placeId is null or placeId = :placeId)
            order by
                case when :sort = 'LATEST' then createdAt end desc,
                case when :sort = 'DISTANCE' then distanceMeters end asc,
                case when :sort = 'CLOSING_SOON' and coalesce(meetingAt, exposureExpiresAt) is null then 1 else 0 end asc,
                case when :sort = 'CLOSING_SOON' then coalesce(meetingAt, exposureExpiresAt) end asc,
                postId desc
            """, nativeQuery = true)
    List<CompanionPostProjection> find(
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("radiusMeters") int radiusMeters,
            @Param("placeCategory") String placeCategory,
            @Param("placeId") Long placeId,
            @Param("sort") String sort
    );
}
