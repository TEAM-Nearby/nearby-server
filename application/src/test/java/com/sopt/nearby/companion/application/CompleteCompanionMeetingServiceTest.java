// 동행 마치기 서비스의 권한, 체크인, 상태 변경을 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingCurrentUserNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompleteCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMeetingIdException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompleteCompanionMeetingServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-29T19:00:00Z"), ZoneOffset.UTC);
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 19, 0);
	private static final Long MEETING_ID = 1L;
	private static final Long MATCH_ID = 10L;
	private static final Long HOST_ID = 1L;
	private static final Long GUEST_ID = 2L;

	private FakeCompanionMeetingRepository meetingRepository;
	private FakeCompanionMatchRepository matchRepository;
	private FakeCompanionMatchParticipantRepository participantRepository;
	private FakeMeetingCheckInRepository checkInRepository;
	private CompleteCompanionMeetingService service;

	@BeforeEach
	void setUp() {
		meetingRepository = new FakeCompanionMeetingRepository();
		matchRepository = new FakeCompanionMatchRepository();
		participantRepository = new FakeCompanionMatchParticipantRepository();
		checkInRepository = new FakeMeetingCheckInRepository();
		service = new CompleteCompanionMeetingService(
				meetingRepository,
				matchRepository,
				participantRepository,
				checkInRepository,
				CLOCK
		);

		meetingRepository.save(meeting(CompanionMeetingStatus.ONGOING));
		matchRepository.save(new CompanionMatch(MATCH_ID, 100L, CompanionMatchStatus.SCHEDULE_CONFIRMED, NOW.minusDays(1)));
		participantRepository.put(MATCH_ID, List.of(
				participant(HOST_ID, MatchParticipantRole.HOST),
				participant(GUEST_ID, MatchParticipantRole.GUEST)
		));
		checkInRepository.put(checkIn(HOST_ID));
		checkInRepository.put(checkIn(GUEST_ID));
	}

	@Test
	void checkedInHostCompletesMeetingAndMatch() {
		CompleteCompanionMeetingResult result = service.complete(MEETING_ID, HOST_ID);

		assertEquals(MEETING_ID, result.meetingId());
		assertEquals(MATCH_ID, result.matchId());
		assertEquals(CompanionMeetingStatus.COMPLETED, result.meetingStatus());
		assertEquals(NOW, result.completedAt());
		assertEquals(CompanionMeetingStatus.COMPLETED, meetingRepository.findById(MEETING_ID).orElseThrow().status());
		assertEquals(CompanionMatchStatus.COMPLETED, matchRepository.findById(MATCH_ID).orElseThrow().status());
	}

	@Test
	void checkedInGuestCanCompleteMeeting() {
		CompleteCompanionMeetingResult result = service.complete(MEETING_ID, GUEST_ID);

		assertEquals(CompanionMeetingStatus.COMPLETED, result.meetingStatus());
	}

	@Test
	void rejectsInvalidMeetingId() {
		assertThrows(InvalidCompanionMeetingIdException.class, () -> service.complete(null, HOST_ID));
		assertThrows(InvalidCompanionMeetingIdException.class, () -> service.complete(0L, HOST_ID));
	}

	@Test
	void rejectsMissingMeeting() {
		meetingRepository.meetings.clear();

		assertThrows(CompanionMeetingNotFoundException.class, () -> service.complete(MEETING_ID, HOST_ID));
	}

	@Test
	void rejectsNonParticipant() {
		assertThrows(
				ForbiddenCompleteCompanionMeetingException.class,
				() -> service.complete(MEETING_ID, 99L)
		);
	}

	@Test
	void rejectsCurrentUserNotCheckedIn() {
		checkInRepository.checkIns.clear();

		assertThrows(
				CompleteCompanionMeetingCurrentUserNotCheckedInException.class,
				() -> service.complete(MEETING_ID, HOST_ID)
		);
	}

	@Test
	void rejectsCanceledMeeting() {
		meetingRepository.save(meeting(CompanionMeetingStatus.CANCELED));

		assertThrows(
				CompleteCompanionMeetingAlreadyCanceledException.class,
				() -> service.complete(MEETING_ID, HOST_ID)
		);
	}

	@Test
	void rejectsCompletedMeeting() {
		meetingRepository.save(meeting(CompanionMeetingStatus.COMPLETED));

		assertThrows(
				CompleteCompanionMeetingAlreadyCompletedException.class,
				() -> service.complete(MEETING_ID, HOST_ID)
		);
	}

	private CompanionMeeting meeting(final CompanionMeetingStatus status) {
		return new CompanionMeeting(MEETING_ID, MATCH_ID, status, NOW.minusHours(1), null);
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
				NOW.minusMinutes(10)
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

	private static final class FakeCompanionMatchRepository implements CompanionMatchRepository {

		private final Map<Long, CompanionMatch> matches = new HashMap<>();

		@Override
		public CompanionMatch save(final CompanionMatch model) {
			matches.put(model.id(), model);
			return model;
		}

		@Override
		public Optional<CompanionMatch> findById(final Long id) {
			return Optional.ofNullable(matches.get(id));
		}

		@Override
		public boolean confirmScheduleIfMatched(final Long matchId) {
			return false;
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
}
