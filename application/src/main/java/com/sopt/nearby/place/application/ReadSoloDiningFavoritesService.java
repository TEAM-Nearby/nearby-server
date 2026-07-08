// 혼밥 맛집 즐겨찾기 목록 조회 유스케이스를 구현한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.exception.InvalidSoloDiningFavoritesRequestException;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSort;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.in.ReadSoloDiningFavoritesUseCase;
import com.sopt.nearby.place.port.out.SoloDiningFavoriteQueryPort;
import java.math.BigDecimal;

public class ReadSoloDiningFavoritesService implements ReadSoloDiningFavoritesUseCase {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final SoloDiningFavoriteQueryPort queryPort;

    public ReadSoloDiningFavoritesService(final SoloDiningFavoriteQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public SoloDiningFavoritesResult read(final ReadSoloDiningFavoritesCommand command) {
        validate(command);

        return new SoloDiningFavoritesResult(queryPort.findAllByUserId(
                command.userId(),
                command.latitude(),
                command.longitude(),
                command.category(),
                command.sort()
        ));
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
