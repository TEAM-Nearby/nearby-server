// 동행 신청 검토 화면 상세 조회 유스케이스를 구현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionRequestNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionRequestHostOnlyException;
import com.sopt.nearby.companion.domain.model.match.CompanionRequestReview;
import com.sopt.nearby.companion.port.in.ReadCompanionRequestReviewUseCase;
import com.sopt.nearby.companion.port.out.CompanionRequestReviewQueryPort;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

public class ReadCompanionRequestReviewService implements ReadCompanionRequestReviewUseCase {

    private final CompanionRequestReviewQueryPort queryPort;

    public ReadCompanionRequestReviewService(final CompanionRequestReviewQueryPort queryPort) {
        this.queryPort = queryPort;
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

        return CompanionRequestReviewResult.from(review, effectiveMeetingAt(review));
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

    private LocalDateTime effectiveMeetingAt(final CompanionRequestReview review) {
        return review.meetingAt() == null ? review.exposureExpiresAt() : review.meetingAt();
    }
}
