// 장소 캐시 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.place.adapter.out.persistence;

import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.mapper.PlacePersistenceMapper;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.place.domain.model.PlaceCache;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;
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
