// 혼밥 맛집 즐겨찾기 목록 조회 유스케이스를 검증한다.
package com.sopt.nearby.place.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningFavoritesRequestException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSort;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.in.ResolvePlaceImageCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.SoloDiningFavoriteQueryPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadSoloDiningFavoritesServiceTest {

    private FakeSoloDiningFavoriteQueryPort queryPort;
    private FakeResolvePlaceImageUseCase resolvePlaceImageUseCase;
    private ReadSoloDiningFavoritesService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeSoloDiningFavoriteQueryPort();
        resolvePlaceImageUseCase = new FakeResolvePlaceImageUseCase();
        service = new ReadSoloDiningFavoritesService(queryPort, resolvePlaceImageUseCase);
    }

    @Test
    void returnsFavoritesFromQueryPort() {
        queryPort.result = List.of(summary(5L, SoloDiningPlaceCategory.CAFE));
        resolvePlaceImageUseCase.result = new ResolvedPlaceImage(
                "https://lh3.googleusercontent.com/place.jpg",
                ResolvedPlaceImage.GOOGLE_MAPS,
                List.of()
        );

        SoloDiningFavoritesResult result = service.read(validCommand(SoloDiningPlaceCategory.CAFE));

        assertEquals(7L, queryPort.userId);
        assertEquals(new BigDecimal("37.56650000"), queryPort.latitude);
        assertEquals(new BigDecimal("126.97800000"), queryPort.longitude);
        assertEquals(SoloDiningPlaceCategory.CAFE, queryPort.category);
        assertEquals(SoloDiningFavoriteSort.LATEST, queryPort.sort);
        assertEquals(1, result.favorites().size());
        assertEquals(5L, result.favorites().get(0).favoriteId());
        assertEquals(true, result.favorites().get(0).isFavorite());
        assertEquals("google-place-id", resolvePlaceImageUseCase.commands.getFirst().googlePlaceId());
        assertEquals("places/google-place-id/photos/photo-resource",
                resolvePlaceImageUseCase.commands.getFirst().photoReference());
        assertEquals("https://lh3.googleusercontent.com/place.jpg", result.favorites().getFirst().imageUrl());
    }

    @Test
    void acceptsMissingCategory() {
        queryPort.result = List.of(summary(5L, null));

        SoloDiningFavoritesResult result = service.read(validCommand(null));

        assertEquals(null, queryPort.category);
        assertEquals(1, result.favorites().size());
        assertEquals(null, result.favorites().get(0).category());
    }

    @Test
    void resolvesFavoriteImagesConcurrentlyWhileKeepingInputOrder() {
        queryPort.result = List.of(
                summary(1L, "google-place-1", "첫 번째 카페"),
                summary(2L, "google-place-2", "두 번째 카페")
        );
        resolvePlaceImageUseCase.expectConcurrentCalls(2);

        SoloDiningFavoritesResult result = service.read(validCommand(SoloDiningPlaceCategory.CAFE));

        assertEquals(1L, result.favorites().get(0).favoriteId());
        assertEquals("첫 번째 카페", result.favorites().get(0).name());
        assertEquals(2L, result.favorites().get(1).favoriteId());
        assertEquals("두 번째 카페", result.favorites().get(1).name());
    }

    @Test
    void rethrowsBusinessExceptionFromParallelImageResolution() {
        queryPort.result = List.of(summary(5L, SoloDiningPlaceCategory.CAFE));
        resolvePlaceImageUseCase.exception = new GooglePlaceApiException();

        assertThrows(GooglePlaceApiException.class,
                () -> service.read(validCommand(SoloDiningPlaceCategory.CAFE)));
    }

    @Test
    void rejectsInvalidCommand() {
        assertThrows(InvalidSoloDiningFavoritesRequestException.class,
                () -> service.read(new ReadSoloDiningFavoritesCommand(
                        null,
                        new BigDecimal("37.56650000"),
                        new BigDecimal("126.97800000"),
                        null,
                        SoloDiningFavoriteSort.LATEST
                )));
        assertThrows(InvalidSoloDiningFavoritesRequestException.class,
                () -> service.read(new ReadSoloDiningFavoritesCommand(
                        7L,
                        new BigDecimal("91.00000000"),
                        new BigDecimal("126.97800000"),
                        null,
                        SoloDiningFavoriteSort.LATEST
                )));
        assertThrows(InvalidSoloDiningFavoritesRequestException.class,
                () -> service.read(new ReadSoloDiningFavoritesCommand(
                        7L,
                        new BigDecimal("37.56650000"),
                        new BigDecimal("126.97800000"),
                        SoloDiningPlaceCategory.OTHER,
                        SoloDiningFavoriteSort.LATEST
                )));
        assertThrows(InvalidSoloDiningFavoritesRequestException.class,
                () -> service.read(new ReadSoloDiningFavoritesCommand(
                        7L,
                        new BigDecimal("37.56650000"),
                        new BigDecimal("126.97800000"),
                        null,
                        null
                )));
    }

    private ReadSoloDiningFavoritesCommand validCommand(final SoloDiningPlaceCategory category) {
        return new ReadSoloDiningFavoritesCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                category,
                SoloDiningFavoriteSort.LATEST
        );
    }

    private SoloDiningFavoriteSummary summary(
            final Long favoriteId,
            final SoloDiningPlaceCategory category
    ) {
        return summary(favoriteId, "google-place-id", "니어바이 카페", category);
    }

    private SoloDiningFavoriteSummary summary(
            final Long favoriteId,
            final String googlePlaceId,
            final String name
    ) {
        return summary(favoriteId, googlePlaceId, name, SoloDiningPlaceCategory.CAFE);
    }

    private SoloDiningFavoriteSummary summary(
            final Long favoriteId,
            final String googlePlaceId,
            final String name,
            final SoloDiningPlaceCategory category
    ) {
        return new SoloDiningFavoriteSummary(
                favoriteId,
                LocalDateTime.of(2026, 7, 2, 13, 20),
                12L,
                googlePlaceId,
                name,
                "서울특별시 중구 세종대로 110",
                "places/google-place-id/photos/photo-resource",
                category,
                80,
                new BigDecimal("4.30"),
                22870,
                true,
                PlaceBusinessStatus.OPERATIONAL
        );
    }

    private static final class FakeSoloDiningFavoriteQueryPort implements SoloDiningFavoriteQueryPort {

        private Long userId;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private SoloDiningPlaceCategory category;
        private SoloDiningFavoriteSort sort;
        private List<SoloDiningFavoriteSummary> result = List.of();

        @Override
        public List<SoloDiningFavoriteSummary> findAllByUserId(
                final Long userId,
                final BigDecimal latitude,
                final BigDecimal longitude,
                final SoloDiningPlaceCategory category,
                final SoloDiningFavoriteSort sort
        ) {
            this.userId = userId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.category = category;
            this.sort = sort;
            return result;
        }
    }

    private static final class FakeResolvePlaceImageUseCase implements ResolvePlaceImageUseCase {

        private final List<ResolvePlaceImageCommand> commands = new CopyOnWriteArrayList<>();
        private ResolvedPlaceImage result = new ResolvedPlaceImage(
                "https://lh3.googleusercontent.com/default.jpg",
                ResolvedPlaceImage.DEFAULT,
                List.of()
        );
        private CountDownLatch concurrentCalls;
        private RuntimeException exception;

        private void expectConcurrentCalls(final int count) {
            concurrentCalls = new CountDownLatch(count);
        }

        @Override
        public ResolvedPlaceImage resolve(final ResolvePlaceImageCommand command) {
            commands.add(command);
            awaitConcurrentCalls();
            if (exception != null) {
                throw exception;
            }
            return result;
        }

        private void awaitConcurrentCalls() {
            if (concurrentCalls == null) {
                return;
            }
            concurrentCalls.countDown();
            try {
                if (!concurrentCalls.await(1, TimeUnit.SECONDS)) {
                    throw new AssertionError("이미지 조회가 병렬로 실행되지 않았습니다.");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("병렬 이미지 조회 대기가 중단되었습니다.", exception);
            }
        }
    }
}
