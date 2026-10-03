// 장소 검색 쿼리 파라미터를 타입이 있는 명령으로 변환한다.
package com.sopt.nearby.place.adapter.in.web.dto.request;

import com.sopt.nearby.place.application.SearchPlacesCommand;
import com.sopt.nearby.place.domain.exception.InvalidPlaceSearchRequestException;
import java.math.BigDecimal;

public record PlaceSearchRequest(String query, String latitude, String longitude,
                                 String radiusMeters, String languageCode, String pageSize, String pageToken) {
    public SearchPlacesCommand toCommand() {
        try {
            return new SearchPlacesCommand(query, decimal(latitude), decimal(longitude),
                    integer(radiusMeters, 20_000), languageCode, integer(pageSize, 20), pageToken);
        } catch (IllegalArgumentException exception) {
            throw new InvalidPlaceSearchRequestException();
        }
    }

    private BigDecimal decimal(final String value) {
        if (value == null || value.length() > 32) {
            throw new InvalidPlaceSearchRequestException();
        }
        return new BigDecimal(value.strip());
    }

    private int integer(final String value, final int defaultValue) {
        return value == null ? defaultValue : Integer.parseInt(value.strip());
    }
}
