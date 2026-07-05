// 외부 장소 이미지 제공자로부터 대표 이미지를 조회하는 포트다.
package com.sopt.nearby.place.port.out;

import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import java.util.Optional;

public interface PlaceImageLookupPort {

    Optional<ResolvedPlaceImage> findImage(String googlePlaceId, String photoReference);
}
