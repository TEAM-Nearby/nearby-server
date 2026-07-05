// 조회된 장소 대표 이미지와 출처 정보를 표현하는 응답 값이다.
package com.sopt.nearby.place.port.in;

import java.util.List;

public record ResolvedPlaceImage(
        String imageUrl,
        String imageSource,
        List<ImageAttribution> imageAttributions
) {

    public static final String GOOGLE_MAPS = "GOOGLE_MAPS";
    public static final String DEFAULT = "DEFAULT";

    public ResolvedPlaceImage {
        imageAttributions = imageAttributions == null ? List.of() : List.copyOf(imageAttributions);
    }

    public record ImageAttribution(
            String displayName,
            String uri,
            String photoUri
    ) {
    }
}
