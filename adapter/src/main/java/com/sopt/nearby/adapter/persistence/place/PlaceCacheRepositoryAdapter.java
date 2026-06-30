// 장소 캐시 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.place;

import com.sopt.nearby.adapter.persistence.place.entity.PlaceCacheEntity;
import com.sopt.nearby.adapter.persistence.place.mapper.PlacePersistenceMapper;
import com.sopt.nearby.adapter.persistence.place.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.place.model.PlaceCache;
import com.sopt.nearby.domain.place.repository.PlaceCacheRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class PlaceCacheRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<PlaceCache, Long, PlaceCacheEntity, Long>
		implements PlaceCacheRepository {

	public PlaceCacheRepositoryAdapter(final PlaceCacheJpaRepository jpaRepository) {
		super(jpaRepository, PlacePersistenceMapper::toEntity, PlacePersistenceMapper::toDomain, Function.identity());
	}
}
