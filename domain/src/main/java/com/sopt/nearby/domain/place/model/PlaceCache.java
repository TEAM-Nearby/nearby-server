// 외부 장소 정보를 캐시하는 도메인 모델
package com.sopt.nearby.domain.place.model;

import java.math.BigDecimal;

public record PlaceCache(
		Long id,
		String googlePlaceId,
		String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		String category,
		String phoneNumber,
		BigDecimal rating,
		Integer reviewCount,
		String photoReference,
		PlaceBusinessStatus businessStatus
) {
}
