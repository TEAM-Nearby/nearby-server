// 혼밥 장소 즐겨찾기 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.place.adapter.out.persistence;

import com.sopt.nearby.place.adapter.out.persistence.entity.SoloDiningFavoriteEntity;
import com.sopt.nearby.place.adapter.out.persistence.mapper.PlacePersistenceMapper;
import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningFavoriteJpaRepository;
import com.sopt.nearby.place.domain.exception.DuplicateSoloDiningFavoriteException;
import com.sopt.nearby.place.domain.model.SoloDiningFavorite;
import com.sopt.nearby.place.port.out.SoloDiningFavoriteRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SoloDiningFavoriteRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<SoloDiningFavorite, Long, SoloDiningFavoriteEntity, Long>
		implements SoloDiningFavoriteRepository {
	private final SoloDiningFavoriteJpaRepository jpaRepository;

	public SoloDiningFavoriteRepositoryAdapter(final SoloDiningFavoriteJpaRepository jpaRepository) {
		super(jpaRepository, PlacePersistenceMapper::toEntity, PlacePersistenceMapper::toDomain, Function.identity());
		this.jpaRepository = jpaRepository;
	}

	@Override
	public SoloDiningFavorite save(final SoloDiningFavorite model) {
		try {
			return PlacePersistenceMapper.toDomain(
					jpaRepository.saveAndFlush(PlacePersistenceMapper.toEntity(model))
			);
		} catch (DataIntegrityViolationException exception) {
			throw mapUniqueConstraintViolation(exception);
		}
	}

	@Override
	public Optional<SoloDiningFavorite> findByUserIdAndPlaceId(final Long userId, final Long placeId) {
		return jpaRepository.findByUserIdAndPlaceId(userId, placeId)
				.map(PlacePersistenceMapper::toDomain);
	}

	@Override
	@Transactional
	public void deleteByUserIdAndPlaceId(final Long userId, final Long placeId) {
		jpaRepository.deleteByUserIdAndPlaceId(userId, placeId);
	}

	private RuntimeException mapUniqueConstraintViolation(final DataIntegrityViolationException exception) {
		String normalizedConstraint = String.valueOf(exception.getMessage()).toLowerCase();
		if (normalizedConstraint.contains("uk_solo_dining_favorite_user_place")
				|| (normalizedConstraint.contains("user_id") && normalizedConstraint.contains("place_id"))) {
			return new DuplicateSoloDiningFavoriteException(exception);
		}
		return exception;
	}
}
