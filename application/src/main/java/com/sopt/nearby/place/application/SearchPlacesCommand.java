// 장소 검색어와 검색 중심 좌표를 정규화하고 검증한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.exception.InvalidPlaceSearchRequestException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

public record SearchPlacesCommand(
        String query, BigDecimal latitude, BigDecimal longitude, int radiusMeters,
        String languageCode, int pageSize, String pageToken
) {
    private static final Set<String> LANGUAGES = Set.of("ko", "en", "es", "fr", "ja", "de", "it", "pt");

    public SearchPlacesCommand {
        query = query == null ? null : query.strip().replaceAll("\\s+", " ");
        languageCode = languageCode == null ? "ko" : languageCode.strip().toLowerCase(Locale.ROOT);
        pageToken = pageToken == null || pageToken.isBlank() ? null : pageToken.strip();
        if (query == null || query.isBlank() || query.codePointCount(0, query.length()) > 200
                || invalidCoordinate(latitude, 90) || invalidCoordinate(longitude, 180)
                || radiusMeters < 1 || radiusMeters > 50_000
                || !LANGUAGES.contains(languageCode) || pageSize < 1 || pageSize > 20
                || (pageToken != null && pageToken.length() > 4096)) {
            throw new InvalidPlaceSearchRequestException();
        }
    }

    private static boolean invalidCoordinate(final BigDecimal value, final int limit) {
        return value == null || value.abs().compareTo(BigDecimal.valueOf(limit)) > 0;
    }
}
