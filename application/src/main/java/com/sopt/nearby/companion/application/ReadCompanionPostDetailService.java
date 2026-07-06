// 동행 모집 글 상세 조회 유스케이스를 구현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionPostExpiredException;
import com.sopt.nearby.companion.domain.exception.CompanionPostNotFoundException;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostApplyStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostDetail;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.port.in.ReadCompanionPostDetailUseCase;
import com.sopt.nearby.companion.port.out.CompanionPostDetailQueryPort;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

public class ReadCompanionPostDetailService implements ReadCompanionPostDetailUseCase {

    private final CompanionPostDetailQueryPort queryPort;
    private final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase;
    private final Clock clock;

    public ReadCompanionPostDetailService(
            final CompanionPostDetailQueryPort queryPort,
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase,
            final Clock clock
    ) {
        this.queryPort = queryPort;
        this.requireCompletedOnboardingUseCase = requireCompletedOnboardingUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanionPostDetailResult read(final ReadCompanionPostDetailCommand command) {
        if (command == null || command.userId() == null || command.postId() == null || command.postId() <= 0) {
            throw new CompanionPostNotFoundException();
        }

        requireCompletedOnboardingUseCase.requireCompleted(command.userId());
        CompanionPostDetail detail = queryPort.findByPostId(command.postId(), command.userId())
                .orElseThrow(CompanionPostNotFoundException::new);
        if (isExpired(detail)) {
            throw new CompanionPostExpiredException();
        }

        CompanionPostApplyStatus applyStatus = toApplyStatus(detail.applicationStatus());
        String openChatUrl = canReadOpenChatUrl(detail, command.userId(), applyStatus)
                ? detail.openChatUrl()
                : null;

        return new CompanionPostDetailResult(
                detail.postId(),
                detail.hostUserId(),
                detail.hostProfileSummary().profileId(),
                detail.place().googlePlaceId(),
                detail.meetingAt(),
                detail.maxParticipants(),
                detail.content(),
                openChatUrl,
                detail.status(),
                detail.createdAt(),
                detail.meetingTimeType(),
                detail.expiresAt(),
                detail.participantCount(),
                applyStatus,
                new CompanionPostDetailResult.Place(
                        detail.place().googlePlaceId(),
                        detail.place().name(),
                        detail.place().address(),
                        detail.place().latitude(),
                        detail.place().longitude(),
                        detail.place().category()
                ),
                new CompanionPostDetailResult.HostProfileSummary(
                        detail.hostProfileSummary().profileId(),
                        detail.hostProfileSummary().nickname(),
                        detail.hostProfileSummary().gender(),
                        detail.hostProfileSummary().birthYear(),
                        detail.hostProfileSummary().profileImageUrl(),
                        detail.hostProfileSummary().mannerScore(),
                        detail.hostProfileSummary().phoneVerifiedAt(),
                        detail.hostProfileSummary().keywords()
                )
        );
    }

    private boolean isExpired(final CompanionPostDetail detail) {
        return switch (detail.meetingTimeType()) {
            case NOW -> isPastOrNow(detail.expiresAt());
            case SCHEDULED -> isPastOrNow(detail.meetingAt());
            case UNDECIDED -> false;
        };
    }

    private boolean isPastOrNow(final LocalDateTime value) {
        return value != null && !value.isAfter(LocalDateTime.now(clock));
    }

    private CompanionPostApplyStatus toApplyStatus(final CompanionApplicationStatus status) {
        if (status == null) {
            return CompanionPostApplyStatus.NOT_APPLIED;
        }
        return CompanionPostApplyStatus.valueOf(status.name());
    }

    private boolean canReadOpenChatUrl(
            final CompanionPostDetail detail,
            final Long userId,
            final CompanionPostApplyStatus applyStatus
    ) {
        return detail.hostUserId().equals(userId) || applyStatus == CompanionPostApplyStatus.ACCEPTED;
    }
}
