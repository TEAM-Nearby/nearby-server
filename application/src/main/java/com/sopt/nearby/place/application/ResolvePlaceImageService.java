// 장소 대표 이미지 조회 유스케이스를 구현한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.port.in.ResolvePlaceImageCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.PlaceImageLookupPort;
import java.util.List;

public class ResolvePlaceImageService implements ResolvePlaceImageUseCase {

    private final PlaceImageLookupPort lookupPort;
    private final String defaultImageUrl;

    public ResolvePlaceImageService(
            final PlaceImageLookupPort lookupPort,
            final String defaultImageUrl
    ) {
        this.lookupPort = lookupPort;
        this.defaultImageUrl = defaultImageUrl;
    }

    @Override
    public ResolvedPlaceImage resolve(final ResolvePlaceImageCommand command) {
        if (command != null && isHttpUrl(command.photoReference())) {
            return googleImage(command.photoReference(), List.of());
        }

        String googlePlaceId = command == null ? null : command.googlePlaceId();
        String photoReference = command == null ? null : command.photoReference();
        return lookupPort.findImage(googlePlaceId, photoReference)
                .orElseGet(this::defaultImage);
    }

    private ResolvedPlaceImage googleImage(
            final String imageUrl,
            final List<ResolvedPlaceImage.ImageAttribution> attributions
    ) {
        return new ResolvedPlaceImage(imageUrl, ResolvedPlaceImage.GOOGLE_MAPS, attributions);
    }

    private ResolvedPlaceImage defaultImage() {
        return new ResolvedPlaceImage(defaultImageUrl, ResolvedPlaceImage.DEFAULT, List.of());
    }

    private boolean isHttpUrl(final String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }
}
