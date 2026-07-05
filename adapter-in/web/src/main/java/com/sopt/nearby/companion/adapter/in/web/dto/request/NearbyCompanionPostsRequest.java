// 주변 동행 모집글 목록 조회 쿼리 파라미터를 유스케이스 명령으로 변환한다.
package com.sopt.nearby.companion.adapter.in.web.dto.request;

import com.sopt.nearby.companion.application.ReadNearbyCompanionPostsCommand;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionPostSearchRequestException;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostSort;
import java.math.BigDecimal;
import java.util.Locale;

public record NearbyCompanionPostsRequest(
        String latitude,
        String longitude,
        String radiusMeters,
        String placeCategory,
        String sort
) {

    public ReadNearbyCompanionPostsCommand toCommand(final Long userId) {
        try {
            return new ReadNearbyCompanionPostsCommand(
                    userId,
                    parseDecimal(latitude),
                    parseDecimal(longitude),
                    Integer.parseInt(required(radiusMeters)),
                    CompanionPostPlaceCategory.valueOf(required(placeCategory).toUpperCase(Locale.ROOT)),
                    CompanionPostSort.valueOf(required(sort).toUpperCase(Locale.ROOT))
            );
        } catch (RuntimeException exception) {
            throw new InvalidCompanionPostSearchRequestException();
        }
    }

    private BigDecimal parseDecimal(final String value) {
        return new BigDecimal(required(value));
    }

    private String required(final String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidCompanionPostSearchRequestException();
        }
        return value;
    }
}
