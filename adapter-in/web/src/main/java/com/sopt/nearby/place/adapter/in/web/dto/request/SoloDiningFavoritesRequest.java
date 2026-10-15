// 혼밥 맛집 즐겨찾기 목록 조회 쿼리 파라미터를 유스케이스 명령으로 변환한다.
package com.sopt.nearby.place.adapter.in.web.dto.request;

import com.sopt.nearby.place.application.ReadSoloDiningFavoritesCommand;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningFavoritesRequestException;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSort;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import java.math.BigDecimal;
import java.util.Locale;

public record SoloDiningFavoritesRequest(
        String latitude,
        String longitude,
        String category,
        String sort
) {

    public ReadSoloDiningFavoritesCommand toCommand(final Long userId) {
        try {
            return new ReadSoloDiningFavoritesCommand(
                    userId,
                    parseDecimal(latitude),
                    parseDecimal(longitude),
                    parseCategory(category),
                    parseSort(sort)
            );
        } catch (RuntimeException exception) {
            throw new InvalidSoloDiningFavoritesRequestException();
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
            throw new InvalidSoloDiningFavoritesRequestException();
        }
        return parsed;
    }

    private SoloDiningFavoriteSort parseSort(final String value) {
        if (value == null || value.isBlank()) {
            return SoloDiningFavoriteSort.LATEST;
        }
        return SoloDiningFavoriteSort.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private String required(final String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidSoloDiningFavoritesRequestException();
        }
        return value;
    }
}
