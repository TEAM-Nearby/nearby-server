// 지원 도시와 일본 주소의 시간대를 유지하고 나머지 장소는 한국 시간대로 계산한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver.ResolvedCityTime;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CompanionPlaceTimeResolver {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final List<String> KOREAN_ADMINISTRATIVE_AREAS = List.of(
            "서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시", "울산광역시",
            "세종특별자치시", "경기도", "강원특별자치도", "강원도", "충청북도", "충청남도", "전북특별자치도",
            "전라북도", "전라남도", "경상북도", "경상남도", "제주특별자치도");
    private static final List<String> JAPANESE_ADMINISTRATIVE_AREAS = List.of(
            "東京都", "北海道", "大阪府", "京都府", "도쿄도", "홋카이도", "오사카부", "교토부", "후쿠오카현");

    public ZoneId resolveZone(
            final String address,
            final BigDecimal latitude,
            final BigDecimal longitude,
            final Instant instant
    ) {
        return CompanionPlaceCityNameResolver.resolveSupportedCity(address)
                .map(city -> city.zoneId())
                .or(() -> countryZone(address))
                .orElse(SEOUL);
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

    private Optional<ZoneId> countryZone(final String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        String normalized = address.toUpperCase(Locale.ROOT);
        if (normalized.matches(".*(?:,|\\s)(?:USA|UNITED STATES|US)\\s*$")) {
            return Optional.of(SEOUL);
        }
        if (normalized.contains("대한민국") || normalized.contains("SOUTH KOREA")
                || KOREAN_ADMINISTRATIVE_AREAS.stream().anyMatch(normalized::contains)
                || normalized.matches(".*(?:^|[,\\s])KR(?:$|[,\\s]).*")) {
            return Optional.of(SEOUL);
        }
        if (normalized.contains("일본") || normalized.contains("日本")
                || normalized.matches(".*(?:^|[,\\s])JAPAN\\s*$")
                || JAPANESE_ADMINISTRATIVE_AREAS.stream().anyMatch(normalized::contains)
                || normalized.contains("県")
                || normalized.matches(".*(?:^|[,\\s])JP(?:$|[,\\s]).*")) {
            return Optional.of(TOKYO);
        }
        return Optional.empty();
    }
}
