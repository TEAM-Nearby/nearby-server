// 조회되거나 저장된 장소 캐시의 식별자를 반환하는 결과 객체다.
package com.sopt.nearby.place.port.in;

public record ResolvedPlaceCache(
        Long placeId
) {
}
