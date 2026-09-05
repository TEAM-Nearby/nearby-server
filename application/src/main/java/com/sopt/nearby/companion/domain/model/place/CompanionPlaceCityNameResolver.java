// 동행 장소 주소에서 화면에 표시할 도시 이름을 추출한다.
package com.sopt.nearby.companion.domain.model.place;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

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

	public static Optional<CompanionCity> resolveSupportedCity(final String address) {
		if (address == null || address.isBlank()) {
			return Optional.empty();
		}

		String normalized = address.toUpperCase(Locale.ROOT);

		return Arrays.stream(CompanionCity.values())
				.filter(city -> city.matches(normalized))
				.findFirst();
	}
}
