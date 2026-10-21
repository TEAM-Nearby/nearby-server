// 동행 매칭 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionMatchJpaRepository extends JpaRepository<CompanionMatchEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select match from CompanionMatchEntity match where match.id = :matchId")
    Optional<CompanionMatchEntity> findByIdForUpdate(@Param("matchId") Long matchId);

    Optional<CompanionMatchEntity> findFirstByPostIdAndStatusOrderByIdAsc(
            Long postId,
            CompanionMatchStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update companion_match
            set status = 'SCHEDULE_CONFIRMED'
            where id = :matchId
                and status = 'MATCHED'
            """, nativeQuery = true)
    int confirmScheduleIfMatched(@Param("matchId") Long matchId);
}
