// 동행 프로필 성향 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileStyleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileStyleEntityId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CompanionProfileStyleJpaRepository
		extends JpaRepository<CompanionProfileStyleEntity, CompanionProfileStyleEntityId> {
    List<CompanionProfileStyleEntity> findAllByProfileIdOrderByKeyword(Long profileId);

    @Modifying(flushAutomatically = true)
    @Query("delete from CompanionProfileStyleEntity style where style.profileId = :profileId")
    void deleteAllByProfileId(Long profileId);
}
