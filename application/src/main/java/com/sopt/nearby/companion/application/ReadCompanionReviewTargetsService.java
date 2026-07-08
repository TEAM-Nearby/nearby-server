// 동행 후기 대상 목록 조회 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReviewMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CurrentUserNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionReviewTargetException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMeetingIdException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.review.CompanionReviewTarget;
import com.sopt.nearby.companion.port.in.ReadCompanionReviewTargetsUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.CompanionReviewTargetQueryPort;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import java.util.List;

public class ReadCompanionReviewTargetsService implements ReadCompanionReviewTargetsUseCase {

	private final CompanionMeetingRepository meetingRepository;
	private final CompanionMatchParticipantRepository participantRepository;
	private final MeetingCheckInRepository checkInRepository;
	private final CompanionReviewTargetQueryPort queryPort;

	public ReadCompanionReviewTargetsService(
			final CompanionMeetingRepository meetingRepository,
			final CompanionMatchParticipantRepository participantRepository,
			final MeetingCheckInRepository checkInRepository,
			final CompanionReviewTargetQueryPort queryPort
	) {
		this.meetingRepository = meetingRepository;
		this.participantRepository = participantRepository;
		this.checkInRepository = checkInRepository;
		this.queryPort = queryPort;
	}

	@Override
	public ReadCompanionReviewTargetsResult getTargets(final Long meetingId, final Long userId) {
		validateMeetingId(meetingId);

		CompanionMeeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(CompanionMeetingNotFoundException::new);
		List<CompanionMatchParticipant> participants = participantRepository.findAllByMatchId(meeting.matchId());
		CompanionMatchParticipant currentUser = participants.stream()
				.filter(participant -> participant.userId().equals(userId))
				.findFirst()
				.orElseThrow(ForbiddenCompanionReviewTargetException::new);

		validateMeetingStatus(meeting.status());
		validateCurrentUserCheckedIn(meeting.id(), userId);

		List<CompanionReviewTarget> targets = queryPort.findAllByMeetingIdAndReviewerUserIdAndTargetRole(
				meeting.id(),
				userId,
				targetRoleOf(currentUser.role())
		);

		return new ReadCompanionReviewTargetsResult(
				meeting.status(),
				currentUser.role(),
				canCompleteMeeting(meeting.status()),
				targets
		);
	}

	private void validateMeetingId(final Long meetingId) {
		if (meetingId == null || meetingId <= 0) {
			throw new InvalidCompanionMeetingIdException();
		}
	}

	private void validateMeetingStatus(final CompanionMeetingStatus meetingStatus) {
		if (meetingStatus == CompanionMeetingStatus.CANCELED) {
			throw new CompanionReviewMeetingAlreadyCanceledException();
		}
	}

	private boolean canCompleteMeeting(final CompanionMeetingStatus meetingStatus) {
		return meetingStatus == CompanionMeetingStatus.ONGOING;
	}

	private void validateCurrentUserCheckedIn(final Long meetingId, final Long userId) {
		if (checkInRepository.findByMeetingIdAndUserId(meetingId, userId).isEmpty()) {
			throw new CurrentUserNotCheckedInException();
		}
	}

	private MatchParticipantRole targetRoleOf(final MatchParticipantRole currentUserRole) {
		if (currentUserRole == MatchParticipantRole.HOST) {
			return MatchParticipantRole.GUEST;
		}
		return MatchParticipantRole.HOST;
	}
}
