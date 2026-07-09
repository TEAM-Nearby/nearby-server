// 호스트의 동행 신청 수락과 거절 처리를 담당한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionRequestNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestNotPendingException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionRequestHostOnlyException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionRequestSelfException;
import com.sopt.nearby.companion.domain.model.match.CompanionApplication;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionSchedule;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.port.in.AcceptCompanionRequestUseCase;
import com.sopt.nearby.companion.port.in.CreateCompanionNotificationUseCase;
import com.sopt.nearby.companion.port.in.RejectCompanionRequestUseCase;
import com.sopt.nearby.companion.port.out.CompanionApplicationRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

public class ProcessCompanionRequestService implements AcceptCompanionRequestUseCase, RejectCompanionRequestUseCase {

    private final CompanionApplicationRepository applicationRepository;
    private final CompanionPostRepository postRepository;
    private final CompanionMatchRepository matchRepository;
    private final CompanionMatchParticipantRepository participantRepository;
    private final CompanionScheduleRepository scheduleRepository;
    private final CreateCompanionNotificationUseCase notificationUseCase;
    private final Clock clock;

    public ProcessCompanionRequestService(
            final CompanionApplicationRepository applicationRepository,
            final CompanionPostRepository postRepository,
            final CompanionMatchRepository matchRepository,
            final CompanionMatchParticipantRepository participantRepository,
            final CompanionScheduleRepository scheduleRepository,
            final CreateCompanionNotificationUseCase notificationUseCase,
            final Clock clock
    ) {
        this.applicationRepository = applicationRepository;
        this.postRepository = postRepository;
        this.matchRepository = matchRepository;
        this.participantRepository = participantRepository;
        this.scheduleRepository = scheduleRepository;
        this.notificationUseCase = notificationUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AcceptedCompanionRequestResult accept(final AcceptCompanionRequestCommand command) {
        validate(command);
        RequestContext context = process(
                command.hostUserId(),
                command.applicationId(),
                CompanionApplicationStatus.ACCEPTED,
                null
        );
        CompanionMatch match = findOrCreateActiveMatchGroup(context.application().postId());
        addParticipantIfMissing(match.id(), context.post().hostUserId(), null, MatchParticipantRole.HOST);
        addParticipantIfMissing(
                match.id(),
                context.application().applicantUserId(),
                context.application().id(),
                MatchParticipantRole.GUEST
        );
        match = confirmNowScheduleIfNeeded(match, context.post());
        notifyApplicant(context.application(), CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED);

        return new AcceptedCompanionRequestResult(
                context.application().id(),
                context.application().postId(),
                CompanionApplicationStatus.ACCEPTED,
                match.id(),
                match.status()
        );
    }

    @Override
    @Transactional
    public RejectedCompanionRequestResult reject(final RejectCompanionRequestCommand command) {
        validate(command);
        RequestContext context = process(
                command.hostUserId(),
                command.applicationId(),
                CompanionApplicationStatus.REJECTED,
                command.rejectionReason()
        );
        notifyApplicant(context.application(), CompanionNotificationType.COMPANION_APPLICATION_REJECTED);

        return new RejectedCompanionRequestResult(
                context.application().id(),
                context.application().postId(),
                CompanionApplicationStatus.REJECTED
        );
    }

    private RequestContext process(
            final Long hostUserId,
            final Long applicationId,
            final CompanionApplicationStatus status,
            final String rejectionReason
    ) {
        CompanionApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(CompanionRequestNotFoundException::new);
        CompanionPost post = findPost(application.postId(), status == CompanionApplicationStatus.ACCEPTED);
        if (!post.hostUserId().equals(hostUserId)) {
            throw new ForbiddenCompanionRequestHostOnlyException();
        }
        if (status == CompanionApplicationStatus.ACCEPTED
                && post.hostUserId().equals(application.applicantUserId())) {
            throw new ForbiddenCompanionRequestSelfException();
        }
        if (application.status() != CompanionApplicationStatus.PENDING) {
            throw new CompanionRequestNotPendingException();
        }
        if (!applicationRepository.updateStatusIfPending(application.id(), status, rejectionReason)) {
            throw new CompanionRequestNotPendingException();
        }
        return new RequestContext(application, post);
    }

    private CompanionPost findPost(final Long postId, final boolean forUpdate) {
        return (forUpdate ? postRepository.findByIdForUpdate(postId) : postRepository.findById(postId))
                .orElseThrow(CompanionRequestNotFoundException::new);
    }

    private CompanionMatch findOrCreateActiveMatchGroup(final Long postId) {
        return matchRepository.findFirstByPostIdAndStatus(postId, CompanionMatchStatus.SCHEDULE_CONFIRMED)
                .or(() -> matchRepository.findFirstByPostIdAndStatus(postId, CompanionMatchStatus.MATCHED))
                .orElseGet(() -> matchRepository.save(new CompanionMatch(
                        null,
                        postId,
                        CompanionMatchStatus.MATCHED,
                        LocalDateTime.now(clock)
                )));
    }

    private CompanionMatch confirmNowScheduleIfNeeded(final CompanionMatch match, final CompanionPost post) {
        if (post.meetingTimeType() != CompanionPostMeetingTimeType.NOW) {
            return match;
        }

        CompanionMatch confirmedMatch = confirmMatchIfNeeded(match);
        if (confirmedMatch.status() == CompanionMatchStatus.SCHEDULE_CONFIRMED) {
            ensureNowSchedule(confirmedMatch.id(), post);
        }
        return confirmedMatch;
    }

    private CompanionMatch confirmMatchIfNeeded(final CompanionMatch match) {
        if (match.status() == CompanionMatchStatus.SCHEDULE_CONFIRMED) {
            return match;
        }

        if (matchRepository.confirmScheduleIfMatched(match.id())) {
            return new CompanionMatch(
                    match.id(),
                    match.postId(),
                    CompanionMatchStatus.SCHEDULE_CONFIRMED,
                    match.createdAt()
            );
        }

        return matchRepository.findById(match.id())
                .filter(latestMatch -> latestMatch.status() == CompanionMatchStatus.SCHEDULE_CONFIRMED)
                .orElse(match);
    }

    private void ensureNowSchedule(final Long matchId, final CompanionPost post) {
        scheduleRepository.findConfirmedByMatchId(matchId)
                .orElseGet(() -> scheduleRepository.save(new CompanionSchedule(
                        null,
                        matchId,
                        post.placeId(),
                        post.exposureExpiresAt(),
                        null,
                        true
                )));
    }

    private void addParticipantIfMissing(
            final Long matchId,
            final Long userId,
            final Long acceptedApplicationId,
            final MatchParticipantRole role
    ) {
        if (participantRepository.existsByMatchIdAndUserId(matchId, userId)) {
            return;
        }
        participantRepository.save(new CompanionMatchParticipant(
                null,
                matchId,
                userId,
                acceptedApplicationId,
                role
        ));
    }

    private void notifyApplicant(
            final CompanionApplication application,
            final CompanionNotificationType notificationType
    ) {
        notificationUseCase.create(new CreateCompanionNotificationCommand(
                application.applicantUserId(),
                notificationType,
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                application.id()
        ));
    }

    private void validate(final AcceptCompanionRequestCommand command) {
        if (command == null) {
            throw new CompanionRequestNotFoundException();
        }
        validateIds(command.hostUserId(), command.applicationId());
    }

    private void validate(final RejectCompanionRequestCommand command) {
        if (command == null) {
            throw new CompanionRequestNotFoundException();
        }
        validateIds(command.hostUserId(), command.applicationId());
    }

    private void validateIds(final Long hostUserId, final Long applicationId) {
        if (hostUserId == null || hostUserId <= 0 || applicationId == null || applicationId <= 0) {
            throw new CompanionRequestNotFoundException();
        }
    }

    private record RequestContext(
            CompanionApplication application,
            CompanionPost post
    ) {
    }
}
