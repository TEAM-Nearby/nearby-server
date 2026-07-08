// 혼밥 맛집 상세 조회 요청값을 유스케이스 명령으로 변환한다.
package com.sopt.nearby.place.adapter.in.web.dto.request;

import com.sopt.nearby.place.application.ReadSoloDiningPlaceCommand;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningPlaceRequestException;
import java.math.BigDecimal;

public record SoloDiningPlaceRequest(
        String placeId,
        String latitude,
        String longitude
) {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    public ReadSoloDiningPlaceCommand toCommand(final Long userId) {
        try {
            Long parsedPlaceId = Long.valueOf(required(placeId));
            BigDecimal parsedLatitude = new BigDecimal(required(latitude));
            BigDecimal parsedLongitude = new BigDecimal(required(longitude));
            if (parsedPlaceId <= 0
                    || parsedLatitude.compareTo(MIN_LATITUDE) < 0
                    || parsedLatitude.compareTo(MAX_LATITUDE) > 0
                    || parsedLongitude.compareTo(MIN_LONGITUDE) < 0
                    || parsedLongitude.compareTo(MAX_LONGITUDE) > 0) {
                throw new InvalidSoloDiningPlaceRequestException();
            }
            return new ReadSoloDiningPlaceCommand(userId, parsedPlaceId, parsedLatitude, parsedLongitude);
        } catch (RuntimeException exception) {
            throw new InvalidSoloDiningPlaceRequestException();
        }
    }

    private String required(final String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidSoloDiningPlaceRequestException();
        }
        return value;
    }
}
