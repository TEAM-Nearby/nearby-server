// 혼밥 맛집 목록 조회 유스케이스를 구현한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningPlacesRequestException;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import com.sopt.nearby.place.port.in.ReadSoloDiningPlacesUseCase;
import com.sopt.nearby.place.port.in.ResolvePlaceImageCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.SoloDiningPlaceQueryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

public class ReadSoloDiningPlacesService implements ReadSoloDiningPlacesUseCase {

    private static final int SEARCH_RADIUS_METERS = 1000;
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final SoloDiningPlaceQueryPort queryPort;
    private final ResolvePlaceImageUseCase resolvePlaceImageUseCase;

    public ReadSoloDiningPlacesService(
            final SoloDiningPlaceQueryPort queryPort,
            final ResolvePlaceImageUseCase resolvePlaceImageUseCase
    ) {
        this.queryPort = queryPort;
        this.resolvePlaceImageUseCase = resolvePlaceImageUseCase;
    }

    @Override
    public SoloDiningPlacesResult read(final ReadSoloDiningPlacesCommand command) {
        validate(command);

        return new SoloDiningPlacesResult(resolveImages(queryPort.findAllNearby(
                command.userId(),
                command.latitude(),
                command.longitude(),
                command.category(),
                SEARCH_RADIUS_METERS
        )));
    }

    private List<SoloDiningPlacesResult.Place> resolveImages(final List<SoloDiningPlaceSummary> places) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<SoloDiningPlacesResult.Place>> futures = places.stream()
                    .map(place -> CompletableFuture.supplyAsync(() -> resolveImage(place), executor))
                    .toList();
            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        } catch (CompletionException exception) {
            if (exception.getCause() instanceof BusinessException businessException) {
                throw businessException;
            }
            throw exception;
        }
    }

    private SoloDiningPlacesResult.Place resolveImage(final SoloDiningPlaceSummary place) {
        ResolvedPlaceImage image = resolvePlaceImageUseCase.resolve(new ResolvePlaceImageCommand(
                place.googlePlaceId(),
                place.photoReference()
        ));
        return SoloDiningPlacesResult.Place.from(place, image.imageUrl());
    }

    private void validate(final ReadSoloDiningPlacesCommand command) {
        if (command == null
                || command.userId() == null
                || command.latitude() == null
                || command.longitude() == null
                || command.category() == SoloDiningPlaceCategory.OTHER
                || command.latitude().compareTo(MIN_LATITUDE) < 0
                || command.latitude().compareTo(MAX_LATITUDE) > 0
                || command.longitude().compareTo(MIN_LONGITUDE) < 0
                || command.longitude().compareTo(MAX_LONGITUDE) > 0) {
            throw new InvalidSoloDiningPlacesRequestException();
        }
    }

}
