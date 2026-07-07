// 혼밥 맛집 목록 조회 쿼리 파라미터를 유스케이스 명령으로 변환한다.
package com.sopt.nearby.place.adapter.in.web.dto.request;

import com.sopt.nearby.place.application.ReadSoloDiningPlacesCommand;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningPlacesRequestException;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import java.math.BigDecimal;
import java.util.Locale;

public record SoloDiningPlacesRequest(
        String latitude,
        String longitude,
        String category
) {

    public ReadSoloDiningPlacesCommand toCommand(final Long userId) {
        try {
            return new ReadSoloDiningPlacesCommand(
                    userId,
                    parseDecimal(latitude),
                    parseDecimal(longitude),
                    parseCategory(category)
            );
        } catch (RuntimeException exception) {
            throw new InvalidSoloDiningPlacesRequestException();
        }
    }

    private BigDecimal parseDecimal(final String value) {
        return new BigDecimal(required(value));
    }

    private SoloDiningPlaceCategory parseCategory(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        SoloDiningPlaceCategory parsed = SoloDiningPlaceCategory.valueOf(value.toUpperCase(Locale.ROOT));
        if (parsed == SoloDiningPlaceCategory.OTHER) {
            throw new InvalidSoloDiningPlacesRequestException();
        }
        return parsed;
    }

    private String required(final String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidSoloDiningPlacesRequestException();
        }
        return value;
    }
}
