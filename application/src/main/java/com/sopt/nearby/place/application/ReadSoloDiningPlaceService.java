// 혼밥 맛집 상세 조회 유스케이스를 구현한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningPlaceRequestException;
import com.sopt.nearby.place.domain.exception.PlaceNotFoundException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import com.sopt.nearby.place.port.in.ReadSoloDiningPlaceUseCase;
import com.sopt.nearby.place.port.in.ResolvePlaceImageCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsResult;
import com.sopt.nearby.place.port.out.SoloDiningPlaceQueryPort;
import java.math.BigDecimal;
import java.util.List;

public class ReadSoloDiningPlaceService implements ReadSoloDiningPlaceUseCase {

    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final SoloDiningPlaceQueryPort queryPort;
    private final SoloDiningPlaceDetailsPort detailsPort;
    private final ResolvePlaceImageUseCase resolvePlaceImageUseCase;

    public ReadSoloDiningPlaceService(
            final SoloDiningPlaceQueryPort queryPort,
            final SoloDiningPlaceDetailsPort detailsPort,
            final ResolvePlaceImageUseCase resolvePlaceImageUseCase
    ) {
        this.queryPort = queryPort;
        this.detailsPort = detailsPort;
        this.resolvePlaceImageUseCase = resolvePlaceImageUseCase;
    }

    @Override
    public SoloDiningPlaceResult read(final ReadSoloDiningPlaceCommand command) {
        validate(command);

        SoloDiningPlaceSummary summary = queryPort.findAllByPlaceIds(
                        command.userId(),
                        command.latitude(),
                        command.longitude(),
                        List.of(command.placeId())
                )
                .stream()
                .findFirst()
                .orElseThrow(PlaceNotFoundException::new);
        SoloDiningPlaceDetailsResult details = detailsPort.findByGooglePlaceId(summary.googlePlaceId());
        if (details == null) {
            throw new GooglePlaceApiException();
        }
        String googlePlaceId = value(details.googlePlaceId(), summary.googlePlaceId());
        String photoReference = value(details.photoReference(), summary.photoReference());
        ResolvedPlaceImage image = resolvePlaceImageUseCase.resolve(new ResolvePlaceImageCommand(
                googlePlaceId,
                photoReference
        ));

        return new SoloDiningPlaceResult(
                summary.placeId(),
                googlePlaceId,
                value(details.name(), summary.name()),
                details.address(),
                value(details.latitude(), summary.latitude()),
                value(details.longitude(), summary.longitude()),
                details.category(),
                summary.distanceMeters(),
                value(details.rating(), summary.rating()),
                value(details.reviewCount(), summary.reviewCount()),
                details.phoneNumber(),
                photoReference,
                list(details.photoReferences()),
                image.imageUrl(),
                businessStatus(details.businessStatus(), summary.businessStatus()),
                unspecifiedToNull(details.priceLevel()),
                details.priceRange(),
                list(details.regularOpeningHours()),
                details.editorialSummary(),
                summary.isFavorite()
        );
    }

    private void validate(final ReadSoloDiningPlaceCommand command) {
        if (command == null
                || command.userId() == null
                || command.userId() <= 0
                || command.placeId() == null
                || command.placeId() <= 0
                || command.latitude() == null
                || command.longitude() == null
                || command.latitude().compareTo(MIN_LATITUDE) < 0
                || command.latitude().compareTo(MAX_LATITUDE) > 0
                || command.longitude().compareTo(MIN_LONGITUDE) < 0
                || command.longitude().compareTo(MAX_LONGITUDE) > 0) {
            throw new InvalidSoloDiningPlaceRequestException();
        }
    }

    private <T> T value(final T value, final T fallback) {
        return value == null ? fallback : value;
    }

    private List<String> list(final List<String> value) {
        return value == null ? List.of() : value;
    }

    private PlaceBusinessStatus businessStatus(
            final PlaceBusinessStatus value,
            final PlaceBusinessStatus fallback
    ) {
        return value(value, value(fallback, PlaceBusinessStatus.UNKNOWN));
    }

    private String unspecifiedToNull(final String value) {
        return "PRICE_LEVEL_UNSPECIFIED".equals(value) ? null : value;
    }
}
