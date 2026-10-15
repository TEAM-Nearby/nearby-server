// 혼밥 맛집 즐겨찾기 상태 변경 명령을 표현한다.
package com.sopt.nearby.place.application;

public record SoloDiningFavoriteCommand(
        Long userId,
        Long placeId
) {
}
