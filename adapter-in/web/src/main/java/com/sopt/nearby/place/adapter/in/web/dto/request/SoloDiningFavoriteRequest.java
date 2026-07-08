// 혼밥 맛집 즐겨찾기 상태 변경 요청값을 유스케이스 명령으로 변환한다.
package com.sopt.nearby.place.adapter.in.web.dto.request;

import com.sopt.nearby.place.application.SoloDiningFavoriteCommand;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningFavoriteRequestException;

public record SoloDiningFavoriteRequest(
        String placeId
) {

    public SoloDiningFavoriteCommand toCommand(final Long userId) {
        String value = required(placeId);
        try {
            Long parsedPlaceId = Long.valueOf(value);
            if (parsedPlaceId <= 0) {
                throw new InvalidSoloDiningFavoriteRequestException();
            }
            return new SoloDiningFavoriteCommand(userId, parsedPlaceId);
        } catch (NumberFormatException exception) {
            throw new InvalidSoloDiningFavoriteRequestException();
        }
    }

    private String required(final String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidSoloDiningFavoriteRequestException();
        }
        return value;
    }
}
