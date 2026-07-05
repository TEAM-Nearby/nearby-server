// 장소 캐시를 조회하거나 저장하는 공개 유스케이스 포트다.
package com.sopt.nearby.place.port.in;

public interface ResolvePlaceCacheUseCase {
    ResolvedPlaceCache resolve(ResolvePlaceCacheCommand command);
}
