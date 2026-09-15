// 동행 푸시 수신 대상 저장용 JPA 저장소다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPushEndpointEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionPushEndpointJpaRepository extends JpaRepository<CompanionPushEndpointEntity, Long> {

    Optional<CompanionPushEndpointEntity> findByUserIdAndInstallationId(Long userId, String installationId);

    List<CompanionPushEndpointEntity> findAllByUserIdAndActiveTrueOrderByIdAsc(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CompanionPushEndpointEntity endpoint
            set endpoint.active = false,
                endpoint.updatedAt = :now
            where endpoint.token = :token
                and endpoint.userId <> :userId
                and endpoint.active = true
            """)
    int deactivateByTokenExceptUser(
            @Param("token") String token,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CompanionPushEndpointEntity endpoint
            set endpoint.active = false,
                endpoint.updatedAt = :now
            where endpoint.id = :endpointId
                and endpoint.active = true
            """)
    int deactivateById(
            @Param("endpointId") Long endpointId,
            @Param("now") LocalDateTime now
    );
}
