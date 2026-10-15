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
	private static final String USER_PLACE_UNIQUE_CONSTRAINT = "uk_solo_dining_favorite_user_place";

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
		if (isUserPlaceUniqueConstraintViolation(exception)) {
			return new DuplicateSoloDiningFavoriteException(exception);
		}
		return exception;
	}

	private boolean isUserPlaceUniqueConstraintViolation(final Throwable exception) {
		Throwable current = exception;
		while (current != null) {
			String message = String.valueOf(current.getMessage()).toLowerCase();
			if (message.contains(USER_PLACE_UNIQUE_CONSTRAINT)) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
