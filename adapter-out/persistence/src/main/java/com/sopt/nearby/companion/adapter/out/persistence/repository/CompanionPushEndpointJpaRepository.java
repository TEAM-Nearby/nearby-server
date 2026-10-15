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
    @Query(value = """
            with token_lock as (
                select pg_advisory_xact_lock(hashtextextended(cast(:token as text), 0))
            ), deactivated as (
                update companion_push_endpoint
                set active = false,
                    updated_at = :now
                where token = :token
                    and user_id <> :userId
                    and active = true
                    and exists (select 1 from token_lock)
            )
            insert into companion_push_endpoint (
                user_id, installation_id, token, platform, active, registration_version,
                last_seen_at, created_at, updated_at
            ) values (
                :userId, :installationId, :token, :platform, true, 1,
                :now, :now, :now
            )
            on conflict (user_id, installation_id) do update
            set token = excluded.token,
                platform = excluded.platform,
                active = true,
                registration_version = case
                    when companion_push_endpoint.token = excluded.token
                        and companion_push_endpoint.platform = excluded.platform
                    then companion_push_endpoint.registration_version
                    else companion_push_endpoint.registration_version + 1
                end,
                last_seen_at = excluded.last_seen_at,
                updated_at = excluded.updated_at
            """, nativeQuery = true)
    int upsert(
            @Param("userId") Long userId,
            @Param("installationId") String installationId,
            @Param("token") String token,
            @Param("platform") String platform,
            @Param("now") LocalDateTime now
    );

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CompanionPushEndpointEntity endpoint
            set endpoint.active = false,
                endpoint.updatedAt = :now
            where endpoint.id = :endpointId
                and endpoint.registrationVersion = :registrationVersion
                and endpoint.token = :token
                and endpoint.active = true
            """)
    int deactivateIfCurrent(
            @Param("endpointId") Long endpointId,
            @Param("registrationVersion") long registrationVersion,
            @Param("token") String token,
            @Param("now") LocalDateTime now
    );
}
