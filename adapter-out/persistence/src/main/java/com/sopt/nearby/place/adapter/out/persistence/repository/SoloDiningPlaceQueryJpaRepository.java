// 혼밥 맛집 목록 조회 네이티브 쿼리를 실행한다.
package com.sopt.nearby.place.adapter.out.persistence.repository;

import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface SoloDiningPlaceQueryJpaRepository extends Repository<PlaceCacheEntity, Long> {

    @Query(value = """
            select
                place.id as placeId,
                place.google_place_id as googlePlaceId,
                place.name as name,
                place.address as address,
                place.photo_reference as photoReference,
                upper(coalesce(place.category, 'OTHER')) as category,
                cast(round(6371000 * acos(least(1.0, greatest(-1.0,
                    cos(radians(:latitude)) * cos(radians(cast(place.latitude as double precision)))
                    * cos(radians(cast(place.longitude as double precision)) - radians(:longitude))
                    + sin(radians(:latitude)) * sin(radians(cast(place.latitude as double precision)))
                )))) as integer) as distanceMeters,
                place.rating as rating,
                place.review_count as reviewCount,
                exists (
                    select 1
                    from solo_dining_favorite favorite
                    where favorite.place_id = place.id
                        and favorite.user_id = :userId
                ) as favorite,
                place.latitude as latitude,
                place.longitude as longitude,
                place.business_status as businessStatus
            from place_cache place
            where (:category is null or upper(coalesce(place.category, 'OTHER')) = :category)
                and 6371000 * acos(least(1.0, greatest(-1.0,
                    cos(radians(:latitude)) * cos(radians(cast(place.latitude as double precision)))
                    * cos(radians(cast(place.longitude as double precision)) - radians(:longitude))
                    + sin(radians(:latitude)) * sin(radians(cast(place.latitude as double precision)))
                ))) <= :radiusMeters
            order by distanceMeters asc, place.id desc
            """, nativeQuery = true)
    List<SoloDiningPlaceProjection> findAllNearby(
            @Param("userId") Long userId,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("category") String category,
            @Param("radiusMeters") int radiusMeters
    );

    @Query(value = """
            select
                place.id as placeId,
                place.google_place_id as googlePlaceId,
                place.name as name,
                place.address as address,
                place.photo_reference as photoReference,
                upper(coalesce(place.category, 'OTHER')) as category,
                cast(round(6371000 * acos(least(1.0, greatest(-1.0,
                    cos(radians(:latitude)) * cos(radians(cast(place.latitude as double precision)))
                    * cos(radians(cast(place.longitude as double precision)) - radians(:longitude))
                    + sin(radians(:latitude)) * sin(radians(cast(place.latitude as double precision)))
                )))) as integer) as distanceMeters,
                place.rating as rating,
                place.review_count as reviewCount,
                exists (
                    select 1
                    from solo_dining_favorite favorite
                    where favorite.place_id = place.id
                        and favorite.user_id = :userId
                ) as favorite,
                place.latitude as latitude,
                place.longitude as longitude,
                place.business_status as businessStatus
            from place_cache place
            where place.id in (:placeIds)
            order by distanceMeters asc, place.id desc
            """, nativeQuery = true)
    List<SoloDiningPlaceProjection> findAllByPlaceIds(
            @Param("userId") Long userId,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("placeIds") List<Long> placeIds
    );

    @Query(value = """
            select
                favorite.id as favoriteId,
                favorite.created_at as createdAt,
                place.id as placeId,
                place.google_place_id as googlePlaceId,
                place.name as name,
                place.address as address,
                place.photo_reference as photoReference,
                upper(place.category) as category,
                cast(round(6371000 * acos(least(1.0, greatest(-1.0,
                    cos(radians(:latitude)) * cos(radians(cast(place.latitude as double precision)))
                    * cos(radians(cast(place.longitude as double precision)) - radians(:longitude))
                    + sin(radians(:latitude)) * sin(radians(cast(place.latitude as double precision)))
                )))) as integer) as distanceMeters,
                place.rating as rating,
                place.review_count as reviewCount,
                place.business_status as businessStatus
            from solo_dining_favorite favorite
            join place_cache place on place.id = favorite.place_id
            where favorite.user_id = :userId
                and (:category is null or upper(place.category) = :category)
            order by
                case when :sort = 'LATEST' then favorite.created_at end desc,
                case when :sort = 'LATEST' then favorite.id end desc,
                case when :sort = 'OLDEST' then favorite.created_at end asc,
                case when :sort = 'OLDEST' then favorite.id end asc
            """, nativeQuery = true)
    List<SoloDiningFavoriteProjection> findAllFavoritesByUserId(
            @Param("userId") Long userId,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("category") String category,
            @Param("sort") String sort
    );
}
