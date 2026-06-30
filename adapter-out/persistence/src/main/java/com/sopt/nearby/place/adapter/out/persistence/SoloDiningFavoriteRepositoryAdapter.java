// 혼밥 장소 즐겨찾기 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.place.adapter.out.persistence;

import com.sopt.nearby.place.adapter.out.persistence.entity.SoloDiningFavoriteEntity;
import com.sopt.nearby.place.adapter.out.persistence.mapper.PlacePersistenceMapper;
import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningFavoriteJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.place.domain.model.SoloDiningFavorite;
import com.sopt.nearby.place.port.out.SoloDiningFavoriteRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class SoloDiningFavoriteRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<SoloDiningFavorite, Long, SoloDiningFavoriteEntity, Long>
		implements SoloDiningFavoriteRepository {

	public SoloDiningFavoriteRepositoryAdapter(final SoloDiningFavoriteJpaRepository jpaRepository) {
		super(jpaRepository, PlacePersistenceMapper::toEntity, PlacePersistenceMapper::toDomain, Function.identity());
	}
}
