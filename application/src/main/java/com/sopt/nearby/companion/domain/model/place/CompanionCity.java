// 동행 지원 도시를 판별하고 해당 도시의 시간대를 제공한다.
package com.sopt.nearby.companion.domain.model.place;

import java.time.ZoneId;
import java.util.List;

public enum CompanionCity {

    MADRID("Europe/Madrid", List.of("MADRID", "마드리드")),
    LONDON("Europe/London", List.of("LONDON", "런던")),
    PARIS("Europe/Paris", List.of("PARIS", "파리"));

    private final ZoneId zoneId;
    private final List<String> addressTokens;

    CompanionCity(final String zoneId, final List<String> addressTokens) {
        this.zoneId = ZoneId.of(zoneId);
        this.addressTokens = addressTokens;
    }

    public ZoneId zoneId() {
        return zoneId;
    }

    boolean matches(final String normalizedAddress) {
        return addressTokens.stream().anyMatch(normalizedAddress::contains);
    }
}
