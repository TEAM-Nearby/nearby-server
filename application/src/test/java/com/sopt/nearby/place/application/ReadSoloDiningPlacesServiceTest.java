// 혼밥 맛집 목록 서비스가 place_cache 근처 조회 결과를 응답으로 조립하는지 검증한다.
package com.sopt.nearby.place.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningPlacesRequestException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import com.sopt.nearby.place.port.in.ResolvePlaceImageCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.SoloDiningPlaceQueryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadSoloDiningPlacesServiceTest {

    private FakeSoloDiningPlaceQueryPort queryPort;
    private FakeResolvePlaceImageUseCase resolvePlaceImageUseCase;
    private ReadSoloDiningPlacesService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeSoloDiningPlaceQueryPort();
        resolvePlaceImageUseCase = new FakeResolvePlaceImageUseCase();
        service = new ReadSoloDiningPlacesService(queryPort, resolvePlaceImageUseCase);
    }

    @Test
    void readsAllNearbyCachedPlacesWithoutResultLimit() {
        queryPort.result = IntStream.rangeClosed(1, 21)
                .mapToObj(index -> summary((long) index, "google-place-" + index, "장소 " + index, false))
                .toList();

        SoloDiningPlacesResult result = service.read(new ReadSoloDiningPlacesCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                SoloDiningPlaceCategory.CAFE
        ));

        assertEquals(7L, queryPort.userId);
        assertEquals(new BigDecimal("37.56650000"), queryPort.latitude);
        assertEquals(new BigDecimal("126.97800000"), queryPort.longitude);
        assertEquals(SoloDiningPlaceCategory.CAFE, queryPort.category);
        assertEquals(1000, queryPort.radiusMeters);
        assertEquals(21, result.places().size());
        assertEquals("장소 1", result.places().getFirst().name());
        assertEquals("장소 21", result.places().getLast().name());
    }

    @Test
    void resolvesImagesConcurrentlyWhileKeepingPlaceOrder() {
        queryPort.result = List.of(
                summary(1L, "google-place-1", "첫 번째 식당", false),
                summary(2L, "google-place-2", "두 번째 식당", true)
        );
        resolvePlaceImageUseCase.expectConcurrentCalls(2);

        SoloDiningPlacesResult result = service.read(validCommand());

        assertEquals("첫 번째 식당", result.places().get(0).name());
        assertEquals("https://lh3.googleusercontent.com/google-place-1.jpg", result.places().get(0).imageUrl());
        assertEquals("두 번째 식당", result.places().get(1).name());
        assertEquals("https://lh3.googleusercontent.com/google-place-2.jpg", result.places().get(1).imageUrl());
    }

    @Test
    void propagatesBusinessExceptionFromParallelImageResolution() {
        queryPort.result = List.of(summary(1L, "google-place-id", "니어바이 카페", false));
        resolvePlaceImageUseCase.exception = new GooglePlaceApiException();

        assertThrows(GooglePlaceApiException.class, () -> service.read(validCommand()));
    }

    @Test
    void rejectsInvalidCommand() {
        assertThrows(InvalidSoloDiningPlacesRequestException.class, () -> service.read(new ReadSoloDiningPlacesCommand(
                7L,
                new BigDecimal("91.00000000"),
                new BigDecimal("126.97800000"),
                null
        )));

        assertThrows(InvalidSoloDiningPlacesRequestException.class, () -> service.read(new ReadSoloDiningPlacesCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                SoloDiningPlaceCategory.OTHER
        )));
    }

    private ReadSoloDiningPlacesCommand validCommand() {
        return new ReadSoloDiningPlacesCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                null
        );
    }

    private SoloDiningPlaceSummary summary(
            final Long placeId,
            final String googlePlaceId,
            final String name,
            final boolean favorite
    ) {
        return new SoloDiningPlaceSummary(
                placeId,
                googlePlaceId,
                name,
                "서울특별시 중구 세종대로 110",
                "places/" + googlePlaceId + "/photos/photo-resource",
                SoloDiningPlaceCategory.CAFE,
                80,
                new BigDecimal("4.30"),
                22870,
                favorite,
                new BigDecimal("37.56612000"),
                new BigDecimal("126.97845000"),
                PlaceBusinessStatus.OPERATIONAL
        );
    }

    private static final class FakeSoloDiningPlaceQueryPort implements SoloDiningPlaceQueryPort {

        private Long userId;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private SoloDiningPlaceCategory category;
        private int radiusMeters;
        private List<SoloDiningPlaceSummary> result = List.of();

        @Override
        public List<SoloDiningPlaceSummary> findAllNearby(
                final Long userId,
                final BigDecimal latitude,
                final BigDecimal longitude,
                final SoloDiningPlaceCategory category,
                final int radiusMeters
        ) {
            this.userId = userId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.category = category;
            this.radiusMeters = radiusMeters;
            return result;
        }

        @Override
        public List<SoloDiningPlaceSummary> findAllByPlaceIds(
                final Long userId,
                final BigDecimal latitude,
                final BigDecimal longitude,
                final List<Long> placeIds
        ) {
            return List.of();
        }
    }

    private static final class FakeResolvePlaceImageUseCase implements ResolvePlaceImageUseCase {

        private final List<ResolvePlaceImageCommand> commands = new CopyOnWriteArrayList<>();
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
            return new ResolvedPlaceImage(
                    "https://lh3.googleusercontent.com/" + command.googlePlaceId() + ".jpg",
                    ResolvedPlaceImage.GOOGLE_MAPS,
                    List.of()
            );
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
