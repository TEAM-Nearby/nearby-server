// 혼밥 맛집 즐겨찾기 등록과 해제 유스케이스를 구현한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.exception.DuplicateSoloDiningFavoriteException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningFavoriteRequestException;
import com.sopt.nearby.place.domain.exception.PlaceNotFoundException;
import com.sopt.nearby.place.domain.model.SoloDiningFavorite;
import com.sopt.nearby.place.port.in.ManageSoloDiningFavoriteUseCase;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;
import com.sopt.nearby.place.port.out.SoloDiningFavoriteRepository;
import java.time.Clock;
import java.time.LocalDateTime;

public class ManageSoloDiningFavoriteService implements ManageSoloDiningFavoriteUseCase {

    private final PlaceCacheRepository placeCacheRepository;
    private final SoloDiningFavoriteRepository favoriteRepository;
    private final Clock clock;

    public ManageSoloDiningFavoriteService(
            final PlaceCacheRepository placeCacheRepository,
            final SoloDiningFavoriteRepository favoriteRepository,
            final Clock clock
    ) {
        this.placeCacheRepository = placeCacheRepository;
        this.favoriteRepository = favoriteRepository;
        this.clock = clock;
    }

    @Override
    public SoloDiningFavoriteResult register(final SoloDiningFavoriteCommand command) {
        validate(command);
        ensurePlaceExists(command.placeId());

        if (favoriteRepository.findByUserIdAndPlaceId(command.userId(), command.placeId()).isEmpty()) {
            saveFavorite(command);
        }

        return new SoloDiningFavoriteResult(true);
    }

    @Override
    public SoloDiningFavoriteResult remove(final SoloDiningFavoriteCommand command) {
        validate(command);
        ensurePlaceExists(command.placeId());

        favoriteRepository.deleteByUserIdAndPlaceId(command.userId(), command.placeId());

        return new SoloDiningFavoriteResult(false);
    }

    private void validate(final SoloDiningFavoriteCommand command) {
        if (command == null
                || command.userId() == null
                || command.userId() <= 0
                || command.placeId() == null
                || command.placeId() <= 0) {
            throw new InvalidSoloDiningFavoriteRequestException();
        }
    }

    private void ensurePlaceExists(final Long placeId) {
        placeCacheRepository.findById(placeId).orElseThrow(PlaceNotFoundException::new);
    }

    private void saveFavorite(final SoloDiningFavoriteCommand command) {
        try {
            favoriteRepository.save(new SoloDiningFavorite(
                    null,
                    command.userId(),
                    command.placeId(),
                    LocalDateTime.now(clock)
            ));
        } catch (DuplicateSoloDiningFavoriteException exception) {
            // 동시 등록 요청이 먼저 저장한 경우에도 등록 API는 멱등 성공으로 처리한다.
        }
    }
}
