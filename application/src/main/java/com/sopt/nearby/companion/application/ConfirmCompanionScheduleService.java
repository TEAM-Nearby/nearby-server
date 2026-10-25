// 글 작성자의 동행 일정 확정 및 수정 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionScheduleException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionScheduleRequestException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionSchedule;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.port.in.ConfirmCompanionScheduleUseCase;
import com.sopt.nearby.companion.port.in.CreateCompanionNotificationUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import org.springframework.transaction.annotation.Transactional;

public class ConfirmCompanionScheduleService implements ConfirmCompanionScheduleUseCase {

    private final CompanionMatchRepository matchRepository;
    private final CompanionPostRepository postRepository;
    private final CompanionScheduleRepository scheduleRepository;
    private final CompanionMeetingRepository meetingRepository;
    private final CompanionMatchParticipantRepository participantRepository;
    private final CreateCompanionNotificationUseCase notificationUseCase;

    public ConfirmCompanionScheduleService(
            final CompanionMatchRepository matchRepository,
            final CompanionPostRepository postRepository,
            final CompanionScheduleRepository scheduleRepository,
            final CompanionMeetingRepository meetingRepository,
            final CompanionMatchParticipantRepository participantRepository,
            final CreateCompanionNotificationUseCase notificationUseCase
    ) {
        this.matchRepository = matchRepository;
        this.postRepository = postRepository;
        this.scheduleRepository = scheduleRepository;
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.notificationUseCase = notificationUseCase;
    }

    @Override
    @Transactional
    public ConfirmCompanionScheduleResult update(final ConfirmCompanionScheduleCommand command) {
        validate(command);

        CompanionMatch match = matchRepository.findByIdForUpdate(command.matchId())
                .orElseThrow(CompanionMatchNotFoundException::new);

        CompanionPost post = postRepository.findById(match.postId())
                .orElseThrow(CompanionMatchNotFoundException::new);

        if (!post.hostUserId().equals(command.requesterUserId())) {
            throw new ForbiddenCompanionScheduleException();
        }

        validateStatus(match.status());

        CompanionSchedule currentSchedule = scheduleRepository.findConfirmedByMatchId(match.id()).orElse(null);
        if (match.status() == CompanionMatchStatus.MATCHED) {
            if (!matchRepository.confirmScheduleIfMatched(match.id())) {
                throw new InvalidCompanionScheduleRequestException();
            }
            CompanionSchedule schedule = scheduleRepository.save(new CompanionSchedule(
                    null,
                    match.id(),
                    post.placeId(),
                    command.scheduledAt(),
                    null,
                    true
            ));
            meetingRepository.save(new CompanionMeeting(
                    null,
                    match.id(),
                    CompanionMeetingStatus.ONGOING,
                    schedule.scheduledAt(),
                    null
            ));
            return result(match.id(), schedule.id());
        }
        if (currentSchedule == null) {
            throw new InvalidCompanionScheduleRequestException();
        }
        CompanionSchedule schedule = scheduleRepository.save(new CompanionSchedule(
                currentSchedule.id(),
                match.id(),
                currentSchedule.placeId(),
                command.scheduledAt(),
                currentSchedule.estimatedDurationMinutes(),
                currentSchedule.confirmed()
        ));
        if (!currentSchedule.scheduledAt().equals(schedule.scheduledAt())) {
            notifyParticipants(match.id());
        }

        return result(match.id(), schedule.id());
    }

    private void notifyParticipants(final Long matchId) {
        participantRepository.findAllByMatchId(matchId).stream()
                .filter(participant -> participant.role() == MatchParticipantRole.GUEST)
                .filter(participant -> participant.acceptedApplicationId() != null)
                .forEach(this::notifyParticipant);
    }

    private void notifyParticipant(final CompanionMatchParticipant participant) {
        notificationUseCase.create(new CreateCompanionNotificationCommand(
                participant.userId(),
                CompanionNotificationType.COMPANION_SCHEDULE_UPDATED,
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                participant.acceptedApplicationId()
        ));
    }

    private void validateStatus(final CompanionMatchStatus status) {
        switch (status) {
            case MATCHED, SCHEDULE_CONFIRMED -> {
            }
            case CANCELED -> throw new CompanionMatchAlreadyCanceledException();
            case COMPLETED -> throw new InvalidCompanionScheduleRequestException();
        }
    }

    private ConfirmCompanionScheduleResult result(final Long matchId, final Long scheduleId) {
        return new ConfirmCompanionScheduleResult(
                matchId,
                scheduleId,
                CompanionMatchStatus.SCHEDULE_CONFIRMED
        );
    }

    private void validate(final ConfirmCompanionScheduleCommand command) {
        if (command == null || command.matchId() == null || command.matchId() <= 0
                || command.requesterUserId() == null || command.scheduledAt() == null) {
            throw new InvalidCompanionScheduleRequestException();
        }
    }
}
