// 장소 대표 이미지 조회에 필요한 Google 장소 식별자를 전달하는 명령 객체다.
package com.sopt.nearby.place.port.in;

public record ResolvePlaceImageCommand(
        String googlePlaceId,
        String photoReference
) {
}
