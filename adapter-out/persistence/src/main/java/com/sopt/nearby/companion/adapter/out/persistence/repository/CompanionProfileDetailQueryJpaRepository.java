// 동행 프로필 상세 조회 네이티브 쿼리를 실행한다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CompanionProfileDetailQueryJpaRepository extends Repository<CompanionProfileEntity, Long> {

    @Query(value = """
            select
                profile.id as profileId,
                profile.user_id as userId,
                profile.nickname as nickname,
                profile.gender as gender,
                profile.birth_year as birthYear,
                profile.profile_image_url as profileImageUrl,
                profile.intro as intro,
                profile.manner_score as mannerScore,
                profile.review_count as reviewCount,
                profile.status as status,
                account.phone_verified_at as phoneVerifiedAt
            from companion_profile profile
            join user_account account
                on account.id = profile.user_id
            where profile.id = :profileId
                and profile.status = 'ACTIVE'
            """, nativeQuery = true)
    Optional<CompanionProfileDetailProjection> findDetailByProfileId(@Param("profileId") Long profileId);

    @Query("""
            select style.keyword
            from CompanionProfileStyleEntity style
            where style.profileId = :profileId
            order by style.keyword
            """)
    List<TravelStyleKeyword> findKeywordsByProfileId(@Param("profileId") Long profileId);
}
