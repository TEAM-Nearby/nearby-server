// 혼밥 맛집 즐겨찾기 목록 조회 유스케이스를 구현한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningFavoritesRequestException;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSort;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.in.ReadSoloDiningFavoritesUseCase;
import com.sopt.nearby.place.port.in.ResolvePlaceImageCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.SoloDiningFavoriteQueryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

public class ReadSoloDiningFavoritesService implements ReadSoloDiningFavoritesUseCase {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final SoloDiningFavoriteQueryPort queryPort;
    private final ResolvePlaceImageUseCase resolvePlaceImageUseCase;

    public ReadSoloDiningFavoritesService(
            final SoloDiningFavoriteQueryPort queryPort,
            final ResolvePlaceImageUseCase resolvePlaceImageUseCase
    ) {
        this.queryPort = queryPort;
        this.resolvePlaceImageUseCase = resolvePlaceImageUseCase;
    }

    @Override
    public SoloDiningFavoritesResult read(final ReadSoloDiningFavoritesCommand command) {
        validate(command);

        return new SoloDiningFavoritesResult(resolveImages(queryPort.findAllByUserId(
                command.userId(),
                command.latitude(),
                command.longitude(),
                command.category(),
                command.sort()
        )));
    }

    private List<SoloDiningFavoritesResult.Favorite> resolveImages(final List<SoloDiningFavoriteSummary> favorites) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<SoloDiningFavoritesResult.Favorite>> futures = favorites.stream()
                    .map(favorite -> CompletableFuture.supplyAsync(() -> resolveImage(favorite), executor))
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

    private SoloDiningFavoritesResult.Favorite resolveImage(final SoloDiningFavoriteSummary favorite) {
        ResolvedPlaceImage image = resolvePlaceImageUseCase.resolve(new ResolvePlaceImageCommand(
                favorite.googlePlaceId(),
                favorite.photoReference()
        ));
        return SoloDiningFavoritesResult.Favorite.from(favorite, image.imageUrl());
    }

    private void validate(final ReadSoloDiningFavoritesCommand command) {
        if (command == null
                || command.userId() == null
                || command.userId() <= 0
                || command.latitude() == null
                || command.longitude() == null
                || command.category() == SoloDiningPlaceCategory.OTHER
                || command.sort() == null
                || command.latitude().compareTo(MIN_LATITUDE) < 0
                || command.latitude().compareTo(MAX_LATITUDE) > 0
                || command.longitude().compareTo(MIN_LONGITUDE) < 0
                || command.longitude().compareTo(MAX_LONGITUDE) > 0) {
            throw new InvalidSoloDiningFavoritesRequestException();
        }
    }
}
