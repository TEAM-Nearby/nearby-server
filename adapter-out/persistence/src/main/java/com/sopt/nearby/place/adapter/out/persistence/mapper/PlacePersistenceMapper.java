// 장소 도메인 모델과 JPA 엔티티 사이의 필드 매핑을 담당하는 클래스
package com.sopt.nearby.place.adapter.out.persistence.mapper;

import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.entity.SoloDiningFavoriteEntity;
import com.sopt.nearby.place.domain.model.PlaceCache;
import com.sopt.nearby.place.domain.model.SoloDiningFavorite;

public final class PlacePersistenceMapper {

	private PlacePersistenceMapper() {
	}

	public static PlaceCacheEntity toEntity(final PlaceCache model) {
		return new PlaceCacheEntity(
				model.id(),
				model.googlePlaceId(),
				model.name(),
				model.address(),
				model.latitude(),
				model.longitude(),
				model.category(),
				model.phoneNumber(),
				model.rating(),
				model.reviewCount(),
				model.photoReference(),
				model.businessStatus()
		);
	}

	public static PlaceCache toDomain(final PlaceCacheEntity entity) {
		return new PlaceCache(
				entity.getId(),
				entity.getGooglePlaceId(),
				entity.getName(),
				entity.getAddress(),
				entity.getLatitude(),
				entity.getLongitude(),
				entity.getCategory(),
				entity.getPhoneNumber(),
				entity.getRating(),
				entity.getReviewCount(),
				entity.getPhotoReference(),
				entity.getBusinessStatus()
		);
	}

	public static SoloDiningFavoriteEntity toEntity(final SoloDiningFavorite model) {
		return new SoloDiningFavoriteEntity(model.id(), model.userId(), model.placeId(), model.createdAt());
	}

	public static SoloDiningFavorite toDomain(final SoloDiningFavoriteEntity entity) {
		return new SoloDiningFavorite(entity.getId(), entity.getUserId(), entity.getPlaceId(), entity.getCreatedAt());
	}
}
