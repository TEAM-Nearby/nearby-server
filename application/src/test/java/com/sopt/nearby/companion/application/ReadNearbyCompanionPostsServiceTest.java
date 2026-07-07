// 주변 동행 모집글 목록 조회 서비스의 검증과 응답 조립을 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionPostSearchRequestException;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostSort;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.post.NearbyCompanionPostSummary;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.NearbyCompanionPostQueryPort;
import com.sopt.nearby.place.port.in.ResolvePlaceImageCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.user.exception.OnboardingRequiredException;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadNearbyCompanionPostsServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-02T04:00:00Z"), ZoneOffset.UTC);

    private FakeNearbyCompanionPostQueryPort queryPort;
    private FakeRequireCompletedOnboardingUseCase onboardingUseCase;
    private FakeResolvePlaceImageUseCase resolvePlaceImageUseCase;
    private ReadNearbyCompanionPostsService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeNearbyCompanionPostQueryPort();
        onboardingUseCase = new FakeRequireCompletedOnboardingUseCase();
        resolvePlaceImageUseCase = new FakeResolvePlaceImageUseCase();
        service = new ReadNearbyCompanionPostsService(
                queryPort,
                onboardingUseCase,
                CLOCK,
                resolvePlaceImageUseCase
        );
    }

    @Test
    void returnsNearbyCompanionPostsWithDisplayText() {
        queryPort.result = List.of(new NearbyCompanionPostSummary(
                101L,
                CompanionPostStatus.RECRUITING,
                "니어바이",
                UserGender.FEMALE,
                20L,
                "google-place-id",
                "니어바이스시",
                CompanionPostPlaceCategory.RESTAURANT,
                new BigDecimal("37.56710000"),
                new BigDecimal("126.97920000"),
                320,
                "https://lh3.googleusercontent.com/place.jpg",
                "12345678901234567890123456789012345678901234567890끝",
                LocalDateTime.of(2026, 7, 3, 14, 0),
                2,
                4,
                LocalDateTime.of(2026, 7, 2, 3, 30)
        ));

        NearbyCompanionPostsResult result = service.read(new ReadNearbyCompanionPostsCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                1000,
                CompanionPostPlaceCategory.ALL,
                null,
                CompanionPostSort.LATEST
        ));

        assertEquals(7L, onboardingUseCase.userId);
        assertEquals(new BigDecimal("37.56650000"), queryPort.command.latitude());
        assertEquals(null, queryPort.command.placeId());
        assertEquals(1000, result.radiusMeters());
        assertEquals(5000, result.maxRadiusMeters());
        assertEquals(CompanionPostPlaceCategory.ALL, result.placeCategory());
        assertEquals(CompanionPostSort.LATEST, result.sort());
        assertEquals(1, result.totalCount());
        assertEquals("내 주변 1개의 동행이 있어요", result.summaryText());

        NearbyCompanionPostsResult.Post post = result.posts().get(0);
        assertEquals(101L, post.postId());
        assertEquals("1234567890123456789012345678901234567890123456789", post.contentPreview());
        assertEquals(true, post.contentPreviewTruncated());
        assertEquals("7월 3일 14:00", post.meetingAtText());
        assertEquals("2/4 모집 중", post.participantSummaryText());
        assertEquals("30분 전", post.createdAgoText());
        assertEquals("7월 3일 14시 니어바이스시 동행", post.mapMarkerText());
        assertEquals("google-place-id", resolvePlaceImageUseCase.command.googlePlaceId());
        assertEquals("https://lh3.googleusercontent.com/place.jpg", resolvePlaceImageUseCase.command.photoReference());
        assertEquals("https://lh3.googleusercontent.com/resolved-place.jpg", post.place().imageUrl());
        assertEquals("GOOGLE_MAPS", post.place().imageSource());
        assertEquals("Google User", post.place().imageAttributions().get(0).displayName());
    }

    @Test
    void usesDefaultImageWhenPlaceHasNoPhotoReference() {
        queryPort.result = List.of(new NearbyCompanionPostSummary(
                101L,
                CompanionPostStatus.RECRUITING,
                "니어바이",
                UserGender.FEMALE,
                20L,
                "google-place-id",
                "니어바이스시",
                CompanionPostPlaceCategory.RESTAURANT,
                new BigDecimal("37.56710000"),
                new BigDecimal("126.97920000"),
                320,
                null,
                "같이 스시 먹어요",
                LocalDateTime.of(2026, 7, 3, 14, 30),
                1,
                4,
                LocalDateTime.of(2026, 7, 1, 3, 30)
        ));
        resolvePlaceImageUseCase.result = new ResolvedPlaceImage(
                "https://cdn.nearby.test/default-place.png",
                "DEFAULT",
                List.of()
        );

        NearbyCompanionPostsResult result = service.read(validCommand());

        assertEquals("https://cdn.nearby.test/default-place.png", result.posts().get(0).place().imageUrl());
        assertEquals("DEFAULT", result.posts().get(0).place().imageSource());
        assertEquals("7월 3일 14시 30분 니어바이스시 동행", result.posts().get(0).mapMarkerText());
        assertEquals("7월 1일", result.posts().get(0).createdAgoText());
    }

    @Test
    void handlesPostWithoutMeetingAt() {
        queryPort.result = List.of(new NearbyCompanionPostSummary(
                101L,
                CompanionPostStatus.RECRUITING,
                "니어바이",
                UserGender.FEMALE,
                20L,
                "google-place-id",
                "니어바이스시",
                CompanionPostPlaceCategory.RESTAURANT,
                new BigDecimal("37.56710000"),
                new BigDecimal("126.97920000"),
                320,
                null,
                "지금 같이 스시 먹어요",
                null,
                1,
                4,
                LocalDateTime.of(2026, 7, 2, 3, 30)
        ));

        NearbyCompanionPostsResult result = service.read(validCommand());

        assertEquals(null, result.posts().get(0).meetingAt());
        assertEquals(null, result.posts().get(0).meetingAtText());
        assertEquals("니어바이스시 동행", result.posts().get(0).mapMarkerText());
    }

    @Test
    void rejectsInvalidSearchCondition() {
        assertThrows(InvalidCompanionPostSearchRequestException.class, () -> service.read(new ReadNearbyCompanionPostsCommand(
                7L,
                new BigDecimal("91.00000000"),
                new BigDecimal("126.97800000"),
                1000,
                CompanionPostPlaceCategory.ALL,
                null,
                CompanionPostSort.LATEST
        )));

        assertThrows(InvalidCompanionPostSearchRequestException.class, () -> service.read(new ReadNearbyCompanionPostsCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                5001,
                CompanionPostPlaceCategory.ALL,
                null,
                CompanionPostSort.LATEST
        )));

        assertThrows(InvalidCompanionPostSearchRequestException.class, () -> service.read(new ReadNearbyCompanionPostsCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                1000,
                CompanionPostPlaceCategory.ALL,
                0L,
                CompanionPostSort.LATEST
        )));
    }

    @Test
    void rejectsUserWithoutCompletedOnboarding() {
        onboardingUseCase.exception = new OnboardingRequiredException();

        assertThrows(OnboardingRequiredException.class, () -> service.read(validCommand()));
    }

    private ReadNearbyCompanionPostsCommand validCommand() {
        return new ReadNearbyCompanionPostsCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                1000,
                CompanionPostPlaceCategory.ALL,
                null,
                CompanionPostSort.LATEST
        );
    }

    private static final class FakeNearbyCompanionPostQueryPort implements NearbyCompanionPostQueryPort {

        private List<NearbyCompanionPostSummary> result = List.of();
        private ReadNearbyCompanionPostsCommand command;

        @Override
        public List<NearbyCompanionPostSummary> findNearby(final ReadNearbyCompanionPostsCommand command) {
            this.command = command;
            return result;
        }
    }

    private static final class FakeRequireCompletedOnboardingUseCase implements RequireCompletedOnboardingUseCase {

        private Long userId;
        private RuntimeException exception;

        @Override
        public void requireCompleted(final Long userId) {
            this.userId = userId;
            if (exception != null) {
                throw exception;
            }
        }
    }

    private static final class FakeResolvePlaceImageUseCase implements ResolvePlaceImageUseCase {

        private ResolvedPlaceImage result = new ResolvedPlaceImage(
                "https://lh3.googleusercontent.com/resolved-place.jpg",
                "GOOGLE_MAPS",
                List.of(new ResolvedPlaceImage.ImageAttribution(
                        "Google User",
                        "https://maps.google.com/contrib/1",
                        "https://lh3.googleusercontent.com/profile.jpg"
                ))
        );
        private ResolvePlaceImageCommand command;

        @Override
        public ResolvedPlaceImage resolve(final ResolvePlaceImageCommand command) {
            this.command = command;
            return result;
        }
    }
}
