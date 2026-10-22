// 혼밥지도에 표시할 영역의 좌표 경계를 검증한다.
package com.sopt.nearby.place.domain.model;

import com.sopt.nearby.place.domain.exception.InvalidSoloDiningPlacesRequestException;
import java.math.BigDecimal;

public record SoloDiningMapBounds(
        BigDecimal southLatitude,
        BigDecimal northLatitude,
        BigDecimal westLongitude,
        BigDecimal eastLongitude
) {

    public SoloDiningMapBounds {
        if (!within(southLatitude, 90) || !within(northLatitude, 90)
                || !within(westLongitude, 180) || !within(eastLongitude, 180)
                || southLatitude.compareTo(northLatitude) >= 0
                || westLongitude.compareTo(eastLongitude) == 0) {
            throw new InvalidSoloDiningPlacesRequestException();
        }
    }

    private static boolean within(final BigDecimal coordinate, final int limit) {
        return coordinate != null
                && coordinate.compareTo(BigDecimal.valueOf(-limit)) >= 0
                && coordinate.compareTo(BigDecimal.valueOf(limit)) <= 0;
    }
}
