// 동행 푸시 수신 대상 포트를 JPA로 구현하는 어댑터다.
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPushEndpointJpaRepository;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushEndpoint;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushPlatform;
import com.sopt.nearby.companion.port.out.CompanionPushEndpointRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CompanionPushEndpointRepositoryAdapter implements CompanionPushEndpointRepository {

    private final CompanionPushEndpointJpaRepository repository;

    public CompanionPushEndpointRepositoryAdapter(final CompanionPushEndpointJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CompanionPushEndpoint> findByUserIdAndInstallationId(
            final Long userId,
            final String installationId
    ) {
        return repository.findByUserIdAndInstallationId(userId, installationId)
                .map(CompanionPersistenceMapper::toDomain);
    }

    @Override
    public List<CompanionPushEndpoint> findActiveByUserId(final Long userId) {
        return repository.findAllByUserIdAndActiveTrueOrderByIdAsc(userId).stream()
                .map(CompanionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public int deactivateByTokenExceptUser(final String token, final Long userId, final LocalDateTime now) {
        return repository.deactivateByTokenExceptUser(token, userId, now);
    }

    @Override
    @Transactional
    public CompanionPushEndpoint upsert(
            final Long userId,
            final String installationId,
            final String token,
            final CompanionPushPlatform platform,
            final LocalDateTime now
    ) {
        repository.deactivateByTokenExceptUser(token, userId, now);
        repository.upsert(userId, installationId, token, platform.name(), now);
        return repository.findByUserIdAndInstallationId(userId, installationId)
                .map(CompanionPersistenceMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("푸시 수신 대상을 저장할 수 없습니다."));
    }

    @Override
    @Transactional
    public int deactivateById(final Long endpointId, final LocalDateTime now) {
        return repository.deactivateById(endpointId, now);
    }

    @Override
    @Transactional
    public int deactivateIfCurrent(
            final Long endpointId,
            final long registrationVersion,
            final String token,
            final LocalDateTime now
    ) {
        return repository.deactivateIfCurrent(endpointId, registrationVersion, token, now);
    }

    @Override
    public CompanionPushEndpoint save(final CompanionPushEndpoint endpoint) {
        return CompanionPersistenceMapper.toDomain(
                repository.saveAndFlush(CompanionPersistenceMapper.toEntity(endpoint))
        );
    }
}
