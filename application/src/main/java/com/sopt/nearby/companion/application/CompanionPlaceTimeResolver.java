// 지원 도시와 장소 좌표·주소를 이용해 장소의 IANA 시간대를 계산한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver.ResolvedCityTime;
import com.sopt.nearby.companion.port.out.CompanionPlaceTimeZoneLookup;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class CompanionPlaceTimeResolver {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

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
                        .orElse(ZoneOffset.UTC));
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
