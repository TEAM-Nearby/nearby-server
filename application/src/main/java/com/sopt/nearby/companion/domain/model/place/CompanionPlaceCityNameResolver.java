// 동행 장소 주소에서 화면에 표시할 도시 이름을 추출한다.
package com.sopt.nearby.companion.domain.model.place;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CompanionPlaceCityNameResolver {

	private static final Set<String> COUNTRY_NAMES = Arrays.stream(Locale.getISOCountries())
			.map(code -> Locale.of("", code))
			.flatMap(country -> Stream.of(
					country.getCountry(), country.getISO3Country(),
					country.getDisplayCountry(Locale.ENGLISH), country.getDisplayCountry(Locale.KOREAN)
			))
			.map(name -> name.toUpperCase(Locale.ROOT))
			.collect(Collectors.toUnmodifiableSet());

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
		int lastIndex = addressParts.length - 1;
		int firstIndex = 0;
		// 실제 국가명으로 끝나는 주소만 도시 위치로 제한해 도로명으로 되돌아가지 않는다.
		if (addressParts.length >= 3 && isCountry(addressParts[lastIndex].trim())) {
			lastIndex--;
			firstIndex = lastIndex;
		}
		for (int index = lastIndex; index >= firstIndex; index--) {
			String addressPart = addressParts[index];
			Optional<CompanionCity> city = Arrays.stream(CompanionCity.values())
					.filter(candidate -> candidate.matches(addressPart))
					.findFirst();
			if (city.isPresent()) {
				return city;
			}
		}
		return Optional.empty();
	}

	private static boolean isCountry(final String addressPart) {
		return COUNTRY_NAMES.contains(addressPart) || "UK".equals(addressPart);
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
