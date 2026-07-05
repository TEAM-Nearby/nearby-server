// 장소 대표 이미지를 조회하는 유스케이스 계약이다.
package com.sopt.nearby.place.port.in;

public interface ResolvePlaceImageUseCase {

    ResolvedPlaceImage resolve(ResolvePlaceImageCommand command);
}
