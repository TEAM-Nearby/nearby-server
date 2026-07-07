// 장소 캐시 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.place.adapter.out.persistence;

import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.mapper.PlacePersistenceMapper;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.place.domain.exception.DuplicatePlaceCacheException;
import com.sopt.nearby.place.domain.model.PlaceCache;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class PlaceCacheRepositoryAdapter
        extends SimpleJpaRepositoryAdapter<PlaceCache, Long, PlaceCacheEntity, Long>
        implements PlaceCacheRepository {
    private final PlaceCacheJpaRepository jpaRepository;

    @Override
    public PlaceCache save(final PlaceCache model) {
        try {
            return PlacePersistenceMapper.toDomain(
                    jpaRepository.saveAndFlush(PlacePersistenceMapper.toEntity(model))
            );
        } catch (DataIntegrityViolationException exception) {
            throw mapUniqueConstraintViolation(exception);
        }
    }

    @Override
    public Optional<PlaceCache> findByGooglePlaceId(final String googlePlaceId) {
        return jpaRepository.findByGooglePlaceId(googlePlaceId)
                .map(PlacePersistenceMapper::toDomain);
    }

    public PlaceCacheRepositoryAdapter(final PlaceCacheJpaRepository jpaRepository) {
        super(jpaRepository, PlacePersistenceMapper::toEntity, PlacePersistenceMapper::toDomain, Function.identity());
        this.jpaRepository = jpaRepository;
    }

    private RuntimeException mapUniqueConstraintViolation(final DataIntegrityViolationException exception) {
        String normalizedConstraint = String.valueOf(exception.getMessage()).toLowerCase();
        if (normalizedConstraint.contains("uk_place_cache_google_place_id")
                || normalizedConstraint.contains("google_place_id")) {
            return new DuplicatePlaceCacheException(exception);
        }
        return exception;
    }
}
