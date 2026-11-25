// 동행 신청 검토 화면 상세 조회 유스케이스를 구현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionRequestNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionRequestHostOnlyException;
import com.sopt.nearby.companion.domain.model.match.CompanionRequestReview;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.port.in.ReadCompanionRequestReviewUseCase;
import com.sopt.nearby.companion.port.out.CompanionRequestReviewQueryPort;
import java.time.ZoneOffset;
import java.time.Clock;
import org.springframework.transaction.annotation.Transactional;

public class ReadCompanionRequestReviewService implements ReadCompanionRequestReviewUseCase {

    private final CompanionRequestReviewQueryPort queryPort;
    private final CompanionPlaceTimeResolver placeTimeResolver;
    private final Clock clock;

    public ReadCompanionRequestReviewService(final CompanionRequestReviewQueryPort queryPort) {
        this(queryPort, new CompanionPlaceTimeResolver(), Clock.systemUTC());
    }

    public ReadCompanionRequestReviewService(
            final CompanionRequestReviewQueryPort queryPort,
            final CompanionPlaceTimeResolver placeTimeResolver,
            final Clock clock
    ) {
        this.queryPort = queryPort;
        this.placeTimeResolver = placeTimeResolver;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanionRequestReviewResult read(final ReadCompanionRequestReviewCommand command) {
        validate(command);

        CompanionRequestReview review = queryPort.findByApplicationId(command.applicationId())
                .orElseThrow(CompanionRequestNotFoundException::new);
        if (!review.hostUserId().equals(command.hostUserId())) {
            throw new ForbiddenCompanionRequestHostOnlyException();
        }

        var zone = placeTimeResolver.resolveZone(
                review.placeAddress(), review.latitude(), review.longitude(), clock.instant());
        return CompanionRequestReviewResult.from(review,
                CompanionMeetingTimeConverter.utcToLocal(review.meetingAt(), zone), zone);
    }

    private void validate(final ReadCompanionRequestReviewCommand command) {
        if (command == null
                || command.hostUserId() == null
                || command.hostUserId() <= 0
                || command.applicationId() == null
                || command.applicationId() <= 0) {
            throw new CompanionRequestNotFoundException();
        }
    }
}
