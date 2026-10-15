// 동행 마치기 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingCurrentUserNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompleteCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMeetingIdException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import com.sopt.nearby.companion.port.in.CompleteCompanionMeetingUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
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

		CompanionMeeting meeting = meetingRepository.findByIdForUpdate(meetingId)
				.orElseThrow(CompanionMeetingNotFoundException::new);
		List<CompanionMatchParticipant> participants = participantRepository.findAllByMatchId(meeting.matchId());
		validateParticipant(participants, userId);
		validateMeetingStatus(meeting.status());
		MeetingCheckIn checkIn = currentUserCheckIn(meeting.id(), userId);
		CompanionMatch match = matchRepository.findById(meeting.matchId())
				.orElseThrow(CompanionMatchNotFoundException::new);
		validateMatchStatus(match.status());

		LocalDateTime currentUserCompletedAt = checkIn.completedAt();
		if (currentUserCompletedAt == null) {
			currentUserCompletedAt = LocalDateTime.now(clock);
			checkInRepository.save(completed(checkIn, currentUserCompletedAt));
		}

		boolean allParticipantsCompleted = checkInRepository.countCompletedByMeetingId(meeting.id())
				== participants.size();
		CompanionMeetingStatus meetingStatus = CompanionMeetingStatus.ONGOING;
		LocalDateTime meetingCompletedAt = null;
		if (allParticipantsCompleted) {
			meetingCompletedAt = currentUserCompletedAt;
			if (!meetingRepository.completeIfOngoing(meeting.id(), meetingCompletedAt)) {
				handleCompletionConflict(meeting.id());
			}
			matchRepository.save(completed(match));
			meetingStatus = CompanionMeetingStatus.COMPLETED;
		}

		return new CompleteCompanionMeetingResult(
				meeting.id(),
				meeting.matchId(),
				true,
				currentUserCompletedAt,
				meetingStatus,
				meetingCompletedAt
		);
	}

	private void validateMeetingId(final Long meetingId) {
		if (meetingId == null || meetingId <= 0) {
			throw new InvalidCompanionMeetingIdException();
		}
	}

	private void validateParticipant(
			final List<CompanionMatchParticipant> participants,
			final Long userId
	) {
		if (participants.stream().noneMatch(participant -> participant.userId().equals(userId))) {
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

	private MeetingCheckIn currentUserCheckIn(final Long meetingId, final Long userId) {
		return checkInRepository.findByMeetingIdAndUserId(meetingId, userId)
				.orElseThrow(CompleteCompanionMeetingCurrentUserNotCheckedInException::new);
	}

	private void validateMatchStatus(final CompanionMatchStatus matchStatus) {
		if (matchStatus == CompanionMatchStatus.CANCELED) {
			throw new CompleteCompanionMeetingAlreadyCanceledException();
		}
		if (matchStatus == CompanionMatchStatus.COMPLETED) {
			throw new CompleteCompanionMeetingAlreadyCompletedException();
		}
	}

	private MeetingCheckIn completed(final MeetingCheckIn checkIn, final LocalDateTime completedAt) {
		return new MeetingCheckIn(
				checkIn.id(),
				checkIn.meetingId(),
				checkIn.userId(),
				checkIn.latitude(),
				checkIn.longitude(),
				checkIn.checkedInAt(),
				completedAt
		);
	}

	private void handleCompletionConflict(final Long meetingId) {
		CompanionMeeting currentMeeting = meetingRepository.findById(meetingId)
				.orElseThrow(CompanionMeetingNotFoundException::new);
		validateMeetingStatus(currentMeeting.status());
		throw new CompleteCompanionMeetingAlreadyCompletedException();
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
