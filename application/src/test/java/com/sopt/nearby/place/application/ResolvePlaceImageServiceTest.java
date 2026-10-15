// 장소 대표 이미지 조회 서비스의 fallback과 기존 URL 처리 규칙을 검증한다.
package com.sopt.nearby.place.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.sopt.nearby.place.port.in.ResolvePlaceImageCommand;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.PlaceImageLookupPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResolvePlaceImageServiceTest {

    private static final String DEFAULT_IMAGE_URL = "https://cdn.nearby.test/default-place.png";

    private final FakePlaceImageLookupPort lookupPort = new FakePlaceImageLookupPort();
    private final ResolvePlaceImageService service = new ResolvePlaceImageService(lookupPort, DEFAULT_IMAGE_URL);

    @Test
    void returnsExistingGooglePhotoUrlWithoutCallingLookupPort() {
        ResolvedPlaceImage result = service.resolve(new ResolvePlaceImageCommand(
                "google-place-id",
                "https://lh3.googleusercontent.com/place.jpg"
        ));

        assertEquals("https://lh3.googleusercontent.com/place.jpg", result.imageUrl());
        assertEquals("GOOGLE_MAPS", result.imageSource());
        assertEquals(0, result.imageAttributions().size());
        assertFalse(lookupPort.called);
    }

    @Test
    void returnsImageFromLookupPort() {
        lookupPort.result = Optional.of(new ResolvedPlaceImage(
                "https://lh3.googleusercontent.com/resolved.jpg",
                "GOOGLE_MAPS",
                List.of(new ResolvedPlaceImage.ImageAttribution(
                        "Google User",
                        "https://maps.google.com/contrib/1",
                        "https://lh3.googleusercontent.com/profile.jpg"
                ))
        ));

        ResolvedPlaceImage result = service.resolve(new ResolvePlaceImageCommand(
                "google-place-id",
                "places/google-place-id/photos/photo-resource"
        ));

        assertEquals("google-place-id", lookupPort.googlePlaceId);
        assertEquals("places/google-place-id/photos/photo-resource", lookupPort.photoReference);
        assertEquals("https://lh3.googleusercontent.com/resolved.jpg", result.imageUrl());
        assertEquals("GOOGLE_MAPS", result.imageSource());
        assertEquals("Google User", result.imageAttributions().get(0).displayName());
    }

    @Test
    void returnsDefaultImageWhenLookupPortReturnsEmpty() {
        ResolvedPlaceImage result = service.resolve(new ResolvePlaceImageCommand("google-place-id", null));

        assertEquals(DEFAULT_IMAGE_URL, result.imageUrl());
        assertEquals("DEFAULT", result.imageSource());
        assertEquals(0, result.imageAttributions().size());
    }

    private static final class FakePlaceImageLookupPort implements PlaceImageLookupPort {

        private Optional<ResolvedPlaceImage> result = Optional.empty();
        private boolean called;
        private String googlePlaceId;
        private String photoReference;

        @Override
        public Optional<ResolvedPlaceImage> findImage(final String googlePlaceId, final String photoReference) {
            called = true;
            this.googlePlaceId = googlePlaceId;
            this.photoReference = photoReference;
            return result;
        }
    }
}
