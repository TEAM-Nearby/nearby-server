// 동행 마치기 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingCurrentUserNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompleteCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMeetingIdException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.port.in.CompleteCompanionMeetingUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

public class CompleteCompanionMeetingService implements CompleteCompanionMeetingUseCase {

	private final CompanionMeetingRepository meetingRepository;
	private final CompanionMatchRepository matchRepository;
	private final CompanionMatchParticipantRepository participantRepository;
	private final MeetingCheckInRepository checkInRepository;
	private final Clock clock;

	public CompleteCompanionMeetingService(
			final CompanionMeetingRepository meetingRepository,
			final CompanionMatchRepository matchRepository,
			final CompanionMatchParticipantRepository participantRepository,
			final MeetingCheckInRepository checkInRepository,
			final Clock clock
	) {
		this.meetingRepository = meetingRepository;
		this.matchRepository = matchRepository;
		this.participantRepository = participantRepository;
		this.checkInRepository = checkInRepository;
		this.clock = clock;
	}

	@Override
	@Transactional
	public CompleteCompanionMeetingResult complete(final Long meetingId, final Long userId) {
		validateMeetingId(meetingId);

		CompanionMeeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(CompanionMeetingNotFoundException::new);
		validateParticipant(meeting.matchId(), userId);
		validateMeetingStatus(meeting.status());
		validateCurrentUserCheckedIn(meeting.id(), userId);

		LocalDateTime completedAt = LocalDateTime.now(clock);
		CompanionMeeting completedMeeting = meetingRepository.save(new CompanionMeeting(
				meeting.id(),
				meeting.matchId(),
				CompanionMeetingStatus.COMPLETED,
				meeting.startedAt(),
				completedAt
		));
		completeMatch(meeting.matchId());

		return new CompleteCompanionMeetingResult(
				completedMeeting.id(),
				completedMeeting.matchId(),
				completedMeeting.status(),
				completedMeeting.completedAt()
		);
	}

	private void validateMeetingId(final Long meetingId) {
		if (meetingId == null || meetingId <= 0) {
			throw new InvalidCompanionMeetingIdException();
		}
	}

	private void validateParticipant(final Long matchId, final Long userId) {
		if (!participantRepository.existsByMatchIdAndUserId(matchId, userId)) {
			throw new ForbiddenCompleteCompanionMeetingException();
		}
	}

	private void validateMeetingStatus(final CompanionMeetingStatus meetingStatus) {
		if (meetingStatus == CompanionMeetingStatus.CANCELED) {
			throw new CompleteCompanionMeetingAlreadyCanceledException();
		}
		if (meetingStatus == CompanionMeetingStatus.COMPLETED) {
			throw new CompleteCompanionMeetingAlreadyCompletedException();
		}
	}

	private void validateCurrentUserCheckedIn(final Long meetingId, final Long userId) {
		if (checkInRepository.findByMeetingIdAndUserId(meetingId, userId).isEmpty()) {
			throw new CompleteCompanionMeetingCurrentUserNotCheckedInException();
		}
	}

	private void completeMatch(final Long matchId) {
		matchRepository.findById(matchId)
				.filter(match -> match.status() != CompanionMatchStatus.CANCELED)
				.filter(match -> match.status() != CompanionMatchStatus.COMPLETED)
				.map(this::completed)
				.ifPresent(matchRepository::save);
	}

	private CompanionMatch completed(final CompanionMatch match) {
		return new CompanionMatch(
				match.id(),
				match.postId(),
				CompanionMatchStatus.COMPLETED,
				match.createdAt()
		);
	}
}
