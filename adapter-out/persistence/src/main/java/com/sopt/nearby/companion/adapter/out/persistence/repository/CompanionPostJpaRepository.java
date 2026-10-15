// 동행 모집글 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionPostJpaRepository extends JpaRepository<CompanionPostEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select post from CompanionPostEntity post where post.id = :id")
    Optional<CompanionPostEntity> findByIdForUpdate(@Param("id") Long id);
}
