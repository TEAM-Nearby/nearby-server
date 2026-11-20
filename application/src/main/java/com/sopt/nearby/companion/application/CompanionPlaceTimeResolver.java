// 지원 도시와 장소 좌표·주소를 이용해 장소의 IANA 시간대를 계산한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver.ResolvedCityTime;
import com.sopt.nearby.companion.port.out.CompanionPlaceTimeZoneLookup;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class CompanionPlaceTimeResolver {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final List<String> KOREAN_ADMINISTRATIVE_AREAS = List.of(
            "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시", "울산광역시",
            "세종특별자치시", "경기도", "강원특별자치도", "강원도", "충청북도", "충청남도", "전북특별자치도",
            "전라북도", "전라남도", "경상북도", "경상남도", "제주특별자치도");

    private final CompanionPlaceTimeZoneLookup timeZoneLookup;

    public CompanionPlaceTimeResolver() {
        this((latitude, longitude, instant) -> Optional.empty());
    }

    public CompanionPlaceTimeResolver(final CompanionPlaceTimeZoneLookup timeZoneLookup) {
        this.timeZoneLookup = timeZoneLookup;
    }

    public ZoneId resolveZone(
            final String address,
            final BigDecimal latitude,
            final BigDecimal longitude,
            final Instant instant
    ) {
        Objects.requireNonNull(instant);
        return CompanionPlaceCityNameResolver.resolveSupportedCity(address)
                .map(city -> city.zoneId())
                .orElseGet(() -> lookup(latitude, longitude, instant)
                        .or(() -> countryZone(address))
                        .orElse(UTC));
    }

    public ResolvedCityTime resolveCurrentTime(
            final String address,
            final BigDecimal latitude,
            final BigDecimal longitude,
            final Instant instant
    ) {
        var city = CompanionPlaceCityNameResolver.resolveSupportedCity(address).orElse(null);
        return new ResolvedCityTime(city, instant.atZone(resolveZone(address, latitude, longitude, instant)));
    }

    private Optional<ZoneId> lookup(
            final BigDecimal latitude,
            final BigDecimal longitude,
            final Instant instant
    ) {
        if (latitude == null || longitude == null) {
            return Optional.empty();
        }
        return timeZoneLookup.findTimeZone(latitude, longitude, instant);
    }

    private Optional<ZoneId> countryZone(final String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        String normalized = address.toUpperCase(Locale.ROOT);
        if (normalized.contains("대한민국") || normalized.contains("SOUTH KOREA")
                || KOREAN_ADMINISTRATIVE_AREAS.stream().anyMatch(normalized::contains)
                || normalized.matches(".*(?:^|[,\\s])KR(?:$|[,\\s]).*")) {
            return Optional.of(SEOUL);
        }
        if (normalized.contains("일본") || normalized.contains("日本") || normalized.contains("JAPAN")
                || normalized.matches(".*(?:^|[,\\s])JP(?:$|[,\\s]).*")) {
            return Optional.of(TOKYO);
        }
        return Optional.empty();
    }
}
