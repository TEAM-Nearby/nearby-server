// 글 작성자의 확정된 동행 일정 수정 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionScheduleException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionScheduleRequestException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionSchedule;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.port.in.ConfirmCompanionScheduleUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import org.springframework.transaction.annotation.Transactional;

public class ConfirmCompanionScheduleService implements ConfirmCompanionScheduleUseCase {

    private final CompanionMatchRepository matchRepository;
    private final CompanionPostRepository postRepository;
    private final CompanionScheduleRepository scheduleRepository;

    public ConfirmCompanionScheduleService(
            final CompanionMatchRepository matchRepository,
            final CompanionPostRepository postRepository,
            final CompanionScheduleRepository scheduleRepository
    ) {
        this.matchRepository = matchRepository;
        this.postRepository = postRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    @Transactional
    public ConfirmCompanionScheduleResult update(final ConfirmCompanionScheduleCommand command) {
        validate(command);

        CompanionMatch match = matchRepository.findById(command.matchId())
                .orElseThrow(CompanionMatchNotFoundException::new);

        CompanionPost post = postRepository.findById(match.postId())
                .orElseThrow(CompanionMatchNotFoundException::new);

        if (!post.hostUserId().equals(command.requesterUserId())) {
            throw new ForbiddenCompanionScheduleException();
        }

        validateStatus(match.status());

        CompanionSchedule currentSchedule = scheduleRepository.findConfirmedByMatchId(match.id())
                .orElseThrow(InvalidCompanionScheduleRequestException::new);
        CompanionSchedule schedule = scheduleRepository.save(new CompanionSchedule(
                currentSchedule.id(),
                match.id(),
                currentSchedule.placeId(),
                command.scheduledAt(),
                currentSchedule.estimatedDurationMinutes(),
                currentSchedule.confirmed()
        ));

        return new ConfirmCompanionScheduleResult(
                match.id(),
                schedule.id(),
                CompanionMatchStatus.SCHEDULE_CONFIRMED
        );
    }

    private void validateStatus(final CompanionMatchStatus status) {
        switch (status) {
            case SCHEDULE_CONFIRMED -> {
            }
            case CANCELED -> throw new CompanionMatchAlreadyCanceledException();
            case MATCHED, COMPLETED -> throw new InvalidCompanionScheduleRequestException();
        }
    }

    private void validate(final ConfirmCompanionScheduleCommand command) {
        if (command == null || command.matchId() == null || command.matchId() <= 0
                || command.requesterUserId() == null || command.scheduledAt() == null) {
            throw new InvalidCompanionScheduleRequestException();
        }
    }
}
