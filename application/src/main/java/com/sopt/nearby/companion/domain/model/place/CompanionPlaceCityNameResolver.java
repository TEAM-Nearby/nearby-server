// 동행 장소 주소에서 화면에 표시할 도시 이름을 추출한다.
package com.sopt.nearby.companion.domain.model.place;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
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

		String[] addressParts = address.toUpperCase(Locale.ROOT).split(",");
		for (int index = addressParts.length - 1; index >= 0; index--) {
			String addressPart = addressParts[index];
			// 쉼표로 구분된 주소는 우편번호를 제외한 도시명만 허용해 도로명 오탐을 막는다.
			String cityPart = addressPart.replaceAll("\\b[\\p{L}\\d]*\\d[\\p{L}\\d]*\\b", "")
					.replaceAll("[^\\p{L}]+", " ").trim();
			if (addressParts.length > 1 && cityPart.contains(" ")) {
				continue;
			}
			Optional<CompanionCity> city = Arrays.stream(CompanionCity.values())
					.filter(candidate -> candidate.matches(addressPart))
					.findFirst();
			if (city.isPresent()) {
				return city;
			}
		}
		return Optional.empty();
	}

	public static ResolvedCityTime resolveCurrentTime(final String address, final Instant instant) {
		Objects.requireNonNull(instant);
		return resolveSupportedCity(address)
				.map(city -> new ResolvedCityTime(city, instant.atZone(city.zoneId())))
				.orElseGet(() -> new ResolvedCityTime(null, null));
	}

	public record ResolvedCityTime(
			CompanionCity city,
			ZonedDateTime currentLocalTime
	) {
	}
}
