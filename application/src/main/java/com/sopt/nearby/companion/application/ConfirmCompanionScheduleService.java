// 글 작성자의 동행 일정 확정 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionScheduleAlreadyConfirmedException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionScheduleException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionScheduleRequestException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionSchedule;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.port.in.ConfirmCompanionScheduleUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheUseCase;
import org.springframework.transaction.annotation.Transactional;

public class ConfirmCompanionScheduleService implements ConfirmCompanionScheduleUseCase {

    private final CompanionMatchRepository matchRepository;
    private final CompanionPostRepository postRepository;
    private final CompanionScheduleRepository scheduleRepository;
    private final CompanionMeetingRepository meetingRepository;
    private final ResolvePlaceCacheUseCase resolvePlaceCacheUseCase;

    public ConfirmCompanionScheduleService(
            final CompanionMatchRepository matchRepository,
            final CompanionPostRepository postRepository,
            final CompanionScheduleRepository scheduleRepository,
            final CompanionMeetingRepository meetingRepository,
            final ResolvePlaceCacheUseCase resolvePlaceCacheUseCase
    ) {
        this.matchRepository = matchRepository;
        this.postRepository = postRepository;
        this.scheduleRepository = scheduleRepository;
        this.meetingRepository = meetingRepository;
        this.resolvePlaceCacheUseCase = resolvePlaceCacheUseCase;
    }

    @Override
    @Transactional
    public ConfirmCompanionScheduleResult confirm(final ConfirmCompanionScheduleCommand command) {
        validate(command);

        CompanionMatch match = matchRepository.findById(command.matchId())
                .orElseThrow(CompanionMatchNotFoundException::new);

        validateStatus(match.status());

        CompanionPost post = postRepository.findById(match.postId())
                .orElseThrow(CompanionMatchNotFoundException::new);

        if (!post.hostUserId().equals(command.requesterUserId())) {
            throw new ForbiddenCompanionScheduleException();
        }

        scheduleRepository.findConfirmedByMatchId(match.id())
                .ifPresent(schedule -> {
                    throw new CompanionScheduleAlreadyConfirmedException();
                });

        confirmMatch(match.id());

        Long placeId = resolvePlace(command.place());

        CompanionSchedule schedule = scheduleRepository.save(new CompanionSchedule(
                null,
                match.id(),
                placeId,
                command.scheduledAt(),
                null,
                true
        ));
        meetingRepository.save(new CompanionMeeting(
                // 신규 데이터이므로 DB가 ID를 생성하라는 뜻이다.
                null,
                match.id(),
                CompanionMeetingStatus.ONGOING,
                schedule.scheduledAt(),
                null
        ));

        postRepository.save(new CompanionPost(
                post.id(),
                post.hostUserId(),
                post.placeId(),
                post.meetingTimeType(),
                post.meetingAt(),
                post.exposureExpiresAt(),
                post.maxParticipants(),
                post.departEvenIfNotFull(),
                post.content(),
                command.openChatUrl(),
                post.status(),
                post.createdAt()
        ));

        return new ConfirmCompanionScheduleResult(
                match.id(),
                schedule.id(),
                CompanionMatchStatus.SCHEDULE_CONFIRMED
        );
    }

    private void validateStatus(final CompanionMatchStatus status) {
        switch (status) {
            case MATCHED -> {
            }
            case SCHEDULE_CONFIRMED -> throw new CompanionScheduleAlreadyConfirmedException();
            case CANCELED -> throw new CompanionMatchAlreadyCanceledException();
            case COMPLETED -> throw new CompanionMatchAlreadyCompletedException();
        }
    }

    private void confirmMatch(final Long matchId) {
        if (matchRepository.confirmScheduleIfMatched(matchId)) {
            return;
        }

        CompanionMatch latestMatch = matchRepository.findById(matchId)
                .orElseThrow(CompanionMatchNotFoundException::new);
        validateStatus(latestMatch.status());
        throw new CompanionScheduleAlreadyConfirmedException();
    }

    private Long resolvePlace(final ConfirmCompanionScheduleCommand.Place place) {
        return resolvePlaceCacheUseCase.resolve(new ResolvePlaceCacheCommand(
                place.googlePlaceId(),
                place.name(),
                place.address(),
                place.latitude(),
                place.longitude()
        )).placeId();
    }

    private void validate(final ConfirmCompanionScheduleCommand command) {
        if (command == null || command.matchId() == null || command.matchId() <= 0
                || command.requesterUserId() == null || command.scheduledAt() == null
                || command.place() == null || isBlank(command.openChatUrl())
                || isBlank(command.place().googlePlaceId()) || isBlank(command.place().name())
                || command.place().latitude() == null || command.place().longitude() == null) {
            throw new InvalidCompanionScheduleRequestException();
        }
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
