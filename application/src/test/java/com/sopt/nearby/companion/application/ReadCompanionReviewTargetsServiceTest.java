// 동행 후기 대상 목록 조회 서비스의 권한과 상태 검증을 확인하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReviewMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CurrentUserNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionReviewTargetException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMeetingIdException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import com.sopt.nearby.companion.domain.model.review.CompanionReviewTarget;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.CompanionReviewTargetQueryPort;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadCompanionReviewTargetsServiceTest {

	private static final Long MEETING_ID = 1L;
	private static final Long MATCH_ID = 10L;
	private static final Long HOST_ID = 1L;
	private static final Long FIRST_GUEST_ID = 2L;
	private static final Long SECOND_GUEST_ID = 3L;
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 18, 35);

	private FakeCompanionMeetingRepository meetingRepository;
	private FakeCompanionMatchParticipantRepository participantRepository;
	private FakeMeetingCheckInRepository checkInRepository;
	private FakeCompanionReviewTargetQueryPort queryPort;
	private ReadCompanionReviewTargetsService service;

	@BeforeEach
	void setUp() {
		meetingRepository = new FakeCompanionMeetingRepository();
		participantRepository = new FakeCompanionMatchParticipantRepository();
		checkInRepository = new FakeMeetingCheckInRepository();
		queryPort = new FakeCompanionReviewTargetQueryPort();
		service = new ReadCompanionReviewTargetsService(
				meetingRepository,
				participantRepository,
				checkInRepository,
				queryPort
		);

		meetingRepository.save(meeting(CompanionMeetingStatus.ONGOING));
		participantRepository.put(MATCH_ID, List.of(
				participant(HOST_ID, MatchParticipantRole.HOST),
				participant(FIRST_GUEST_ID, MatchParticipantRole.GUEST),
				participant(SECOND_GUEST_ID, MatchParticipantRole.GUEST)
		));
		checkInRepository.put(checkIn(HOST_ID));
		checkInRepository.put(checkIn(FIRST_GUEST_ID));
		queryPort.targets = List.of(target(FIRST_GUEST_ID, false), target(SECOND_GUEST_ID, true));
	}

	@Test
	void hostReadsCheckedInGuestTargets() {
		ReadCompanionReviewTargetsResult result = service.getTargets(MEETING_ID, HOST_ID);

		assertEquals(CompanionMeetingStatus.ONGOING, result.meetingStatus());
		assertEquals(MatchParticipantRole.HOST, result.currentUserRole());
		assertTrue(result.canCompleteMeeting());
		assertEquals(2, result.reviewTargets().size());
		assertEquals(MatchParticipantRole.GUEST, queryPort.targetRole);
		assertEquals(HOST_ID, queryPort.reviewerUserId);
	}

	@Test
	void guestReadsHostTarget() {
		queryPort.targets = List.of(target(HOST_ID, false));

		ReadCompanionReviewTargetsResult result = service.getTargets(MEETING_ID, FIRST_GUEST_ID);

		assertEquals(MatchParticipantRole.GUEST, result.currentUserRole());
		assertEquals(MatchParticipantRole.HOST, queryPort.targetRole);
		assertEquals(HOST_ID, result.reviewTargets().get(0).revieweeUserId());
	}

	@Test
	void rejectsInvalidMeetingId() {
		assertThrows(InvalidCompanionMeetingIdException.class, () -> service.getTargets(null, HOST_ID));
		assertThrows(InvalidCompanionMeetingIdException.class, () -> service.getTargets(0L, HOST_ID));
	}

	@Test
	void rejectsMissingMeeting() {
		meetingRepository.meetings.clear();

		assertThrows(CompanionMeetingNotFoundException.class, () -> service.getTargets(MEETING_ID, HOST_ID));
	}

	@Test
	void rejectsNonParticipant() {
		assertThrows(
				ForbiddenCompanionReviewTargetException.class,
				() -> service.getTargets(MEETING_ID, 99L)
		);
	}

	@Test
	void rejectsCurrentUserNotCheckedIn() {
		assertThrows(
				CurrentUserNotCheckedInException.class,
				() -> service.getTargets(MEETING_ID, SECOND_GUEST_ID)
		);
	}

	@Test
	void rejectsCanceledMeeting() {
		meetingRepository.save(meeting(CompanionMeetingStatus.CANCELED));

		assertThrows(
				CompanionReviewMeetingAlreadyCanceledException.class,
				() -> service.getTargets(MEETING_ID, HOST_ID)
		);
	}

	@Test
	void allowsCompletedMeetingForGuest() {
		meetingRepository.save(meeting(CompanionMeetingStatus.COMPLETED));

		ReadCompanionReviewTargetsResult result = service.getTargets(MEETING_ID, FIRST_GUEST_ID);

		assertEquals(CompanionMeetingStatus.COMPLETED, result.meetingStatus());
		assertEquals(MatchParticipantRole.GUEST, result.currentUserRole());
		assertFalse(result.canCompleteMeeting());
	}

	@Test
	void allowsCompletedMeetingForHost() {
		meetingRepository.save(meeting(CompanionMeetingStatus.COMPLETED));

		ReadCompanionReviewTargetsResult result = service.getTargets(MEETING_ID, HOST_ID);

		assertEquals(CompanionMeetingStatus.COMPLETED, result.meetingStatus());
		assertEquals(MatchParticipantRole.HOST, result.currentUserRole());
		assertFalse(result.canCompleteMeeting());
	}

	private CompanionMeeting meeting(final CompanionMeetingStatus status) {
		return new CompanionMeeting(MEETING_ID, MATCH_ID, status, NOW.minusMinutes(5), null);
	}

	private CompanionMatchParticipant participant(final Long userId, final MatchParticipantRole role) {
		return new CompanionMatchParticipant(null, MATCH_ID, userId, null, role);
	}

	private MeetingCheckIn checkIn(final Long userId) {
		return new MeetingCheckIn(
				null,
				MEETING_ID,
				userId,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				NOW.minusMinutes(1)
		);
	}

	private CompanionReviewTarget target(final Long revieweeUserId, final boolean hasWrittenReview) {
		return new CompanionReviewTarget(
				revieweeUserId,
				"https://image.url/profile-" + revieweeUserId + ".png",
				"동행자" + revieweeUserId,
				"바르셀로나",
				LocalDate.of(2026, 6, 18),
				true,
				hasWrittenReview
		);
	}

	private static final class FakeCompanionMeetingRepository implements CompanionMeetingRepository {

		private final Map<Long, CompanionMeeting> meetings = new HashMap<>();

		@Override
		public CompanionMeeting save(final CompanionMeeting model) {
			meetings.put(model.id(), model);
			return model;
		}

		@Override
		public Optional<CompanionMeeting> findById(final Long id) {
			return Optional.ofNullable(meetings.get(id));
		}
	}

	private static final class FakeCompanionMatchParticipantRepository
			implements CompanionMatchParticipantRepository {

		private final Map<Long, List<CompanionMatchParticipant>> participants = new HashMap<>();

		@Override
		public CompanionMatchParticipant save(final CompanionMatchParticipant model) {
			participants.computeIfAbsent(model.matchId(), ignored -> new ArrayList<>()).add(model);
			return model;
		}

		@Override
		public Optional<CompanionMatchParticipant> findById(final Long id) {
			return participants.values().stream()
					.flatMap(List::stream)
					.filter(participant -> participant.id() != null && participant.id().equals(id))
					.findFirst();
		}

		@Override
		public List<CompanionMatchParticipant> findAllByMatchId(final Long matchId) {
			return participants.getOrDefault(matchId, List.of());
		}

		@Override
		public boolean existsByMatchIdAndUserId(final Long matchId, final Long userId) {
			return findAllByMatchId(matchId).stream()
					.anyMatch(participant -> participant.userId().equals(userId));
		}

		private void put(final Long matchId, final List<CompanionMatchParticipant> values) {
			participants.put(matchId, values);
		}
	}

	private static final class FakeMeetingCheckInRepository implements MeetingCheckInRepository {

		private final Map<Key, MeetingCheckIn> checkIns = new HashMap<>();

		@Override
		public MeetingCheckIn save(final MeetingCheckIn model) {
			checkIns.put(new Key(model.meetingId(), model.userId()), model);
			return model;
		}

		@Override
		public Optional<MeetingCheckIn> findById(final Long id) {
			return checkIns.values().stream()
					.filter(checkIn -> checkIn.id() != null && checkIn.id().equals(id))
					.findFirst();
		}

		@Override
		public Optional<MeetingCheckIn> findByMeetingIdAndUserId(final Long meetingId, final Long userId) {
			return Optional.ofNullable(checkIns.get(new Key(meetingId, userId)));
		}

		@Override
		public long countByMeetingId(final Long meetingId) {
			return checkIns.values().stream()
					.filter(checkIn -> checkIn.meetingId().equals(meetingId))
					.count();
		}

		@Override
		public MeetingCheckIn saveIfAbsent(final MeetingCheckIn checkIn) {
			return findByMeetingIdAndUserId(checkIn.meetingId(), checkIn.userId())
					.orElseGet(() -> save(checkIn));
		}

		private void put(final MeetingCheckIn checkIn) {
			save(checkIn);
		}

		private record Key(Long meetingId, Long userId) {
		}
	}

	private static final class FakeCompanionReviewTargetQueryPort implements CompanionReviewTargetQueryPort {

		private List<CompanionReviewTarget> targets = List.of();
		private Long reviewerUserId;
		private MatchParticipantRole targetRole;

		@Override
		public List<CompanionReviewTarget> findAllByMeetingIdAndReviewerUserIdAndTargetRole(
				final Long meetingId,
				final Long reviewerUserId,
				final MatchParticipantRole targetRole
		) {
			this.reviewerUserId = reviewerUserId;
			this.targetRole = targetRole;
			return targets;
		}
	}
}
