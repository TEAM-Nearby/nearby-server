// 동행 장소 주소에서 화면에 표시할 도시 이름을 추출한다.
package com.sopt.nearby.companion.domain.model.place;

public final class CompanionPlaceCityNameResolver {

	private CompanionPlaceCityNameResolver() {
	}

	public static String resolve(final String placeAddress) {
		return resolve(placeAddress, "");
	}

	public static String resolve(final String placeAddress, final String fallbackName) {
		String fallback = fallbackName == null ? "" : fallbackName;
		if (placeAddress == null || placeAddress.isBlank()) {
			return fallback;
		}

		String normalized = placeAddress.trim().replace(",", " ").trim();
		if (normalized.isBlank()) {
			return fallback;
		}

		int firstBlankIndex = normalized.indexOf(' ');
		if (firstBlankIndex < 0) {
			return normalized;
		}
		return normalized.substring(0, firstBlankIndex);
	}
}
