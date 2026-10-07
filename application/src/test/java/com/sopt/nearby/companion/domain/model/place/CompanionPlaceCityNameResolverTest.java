// 장소 주소의 지원 도시 판별과 현재 현지 시각 변환을 검증한다.
package com.sopt.nearby.companion.domain.model.place;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver.ResolvedCityTime;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompanionPlaceCityNameResolverTest {

	private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

	@ParameterizedTest
	@CsvSource({
			"'Rambla de Catalunya, 18, Barcelona, Spain', 2026-07-01T12:00:00Z, 2026-07-01T14:00+02:00",
			"'스페인 바르셀로나', 2026-01-01T12:00:00Z, 2026-01-01T13:00+01:00"
	})
	void resolvesBarcelonaWithSeasonalOffset(String address, String instant, String localTime) {
		ResolvedCityTime result = CompanionPlaceCityNameResolver.resolveCurrentTime(address, Instant.parse(instant));

		assertEquals(CompanionCity.BARCELONA, result.city());
		assertEquals("Europe/Madrid", result.city().zoneId().getId());
		assertEquals(localTime, result.currentLocalTime().toOffsetDateTime().toString());
	}

	@Test
	void resolvesSupportedCityWithDaylightSavingTime() {
		ResolvedCityTime result = CompanionPlaceCityNameResolver.resolveCurrentTime(
				"Calle de Cuchilleros, 17, Madrid, Spain",
				NOW
		);

		assertEquals(CompanionCity.MADRID, result.city());
		assertEquals("2026-07-01T14:00+02:00", result.currentLocalTime().toOffsetDateTime().toString());
	}

	@Test
	void prefersCityAddressPartOverStreetName() {
		ResolvedCityTime result = CompanionPlaceCityNameResolver.resolveCurrentTime(
				"Madrid Road, London, UK",
				NOW
		);

		assertEquals(CompanionCity.LONDON, result.city());
		assertEquals("2026-07-01T13:00+01:00", result.currentLocalTime().toOffsetDateTime().toString());
	}

	@Test
	void returnsNullFieldsForUnsupportedCity() {
		ResolvedCityTime result = CompanionPlaceCityNameResolver.resolveCurrentTime(
				"Via dei Giubbonari, 21, Rome, Italy",
				NOW
		);

		assertNull(result.city());
		assertNull(result.currentLocalTime());
	}
}
