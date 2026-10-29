// 동행 지원 도시를 판별하고 해당 도시의 시간대를 제공한다.
package com.sopt.nearby.companion.domain.model.place;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

public enum CompanionCity {

    BARCELONA("Europe/Madrid", "Barcelona", "바르셀로나", List.of("BARCELONA", "바르셀로나"),
            List.of("CATALONIA", "CATALUNYA", "CATALUÑA", "카탈루냐")),
    MADRID("Europe/Madrid", "Madrid", "마드리드", List.of("MADRID", "마드리드"),
            List.of("COMMUNITY OF MADRID", "COMUNIDAD DE MADRID", "마드리드 자치주")),
    LONDON("Europe/London", "London", "런던", List.of("LONDON", "런던"),
            List.of("GREATER LONDON", "ENGLAND", "잉글랜드")),
    PARIS("Europe/Paris", "Paris", "파리", List.of("PARIS", "파리"),
            List.of("ÎLE-DE-FRANCE", "ILE-DE-FRANCE", "ÎLE DE FRANCE", "ILE DE FRANCE", "일드프랑스"));

    private final ZoneId zoneId;
    private final String displayName;
    private final String koreanName;
    private final List<String> addressTokens;
    private final List<String> regionTokens;

    CompanionCity(
            final String zoneId,
            final String displayName,
            final String koreanName,
            final List<String> addressTokens,
            final List<String> regionTokens
    ) {
        this.zoneId = ZoneId.of(zoneId);
        this.displayName = displayName;
        this.koreanName = koreanName;
        this.addressTokens = addressTokens;
        this.regionTokens = regionTokens;
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    public String displayName() {
        return displayName;
    }

    public String koreanName() {
        return koreanName;
    }

    boolean matches(final String normalizedAddressPart) {
        List<String> words = Arrays.asList(normalizedAddressPart.split("[^\\p{L}]+"));
        return addressTokens.stream().anyMatch(words::contains);
    }

    boolean matchesRegion(final String normalizedAddressPart) {
        return regionTokens.contains(normalizedAddressPart.trim());
    }
}
