// 동행 후기 등록 서비스의 대상 검증, 키워드 검증, 반복 등록 방지를 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sopt.nearby.companion.domain.exception.CompanionReviewAlreadyExistsException;
import com.sopt.nearby.companion.domain.exception.InvalidReviewKeywordCountException;
import com.sopt.nearby.companion.domain.exception.InvalidReviewTargetException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import com.sopt.nearby.companion.domain.model.review.CompanionReview;
import com.sopt.nearby.companion.domain.model.review.CompanionReviewKeyword;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.CompanionReviewKeywordRepository;
import com.sopt.nearby.companion.port.out.CompanionReviewRepository;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateCompanionReviewsServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-29T18:35:00Z"), ZoneOffset.UTC);
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 18, 35);
	private static final Long MEETING_ID = 1L;
	private static final Long MATCH_ID = 10L;
	private static final Long HOST_ID = 1L;
	private static final Long FIRST_GUEST_ID = 2L;
	private static final Long SECOND_GUEST_ID = 3L;

	private FakeCompanionMeetingRepository meetingRepository;
	private FakeCompanionMatchParticipantRepository participantRepository;
	private FakeMeetingCheckInRepository checkInRepository;
	private FakeCompanionReviewRepository reviewRepository;
	private FakeCompanionReviewKeywordRepository reviewKeywordRepository;
	private CreateCompanionReviewsService service;

	@BeforeEach
	void setUp() {
		meetingRepository = new FakeCompanionMeetingRepository();
		participantRepository = new FakeCompanionMatchParticipantRepository();
		checkInRepository = new FakeMeetingCheckInRepository();
		reviewRepository = new FakeCompanionReviewRepository();
		reviewKeywordRepository = new FakeCompanionReviewKeywordRepository();
		service = new CreateCompanionReviewsService(
				meetingRepository,
				participantRepository,
				checkInRepository,
				reviewRepository,
				reviewKeywordRepository,
				CLOCK
		);

		meetingRepository.save(meeting(CompanionMeetingStatus.ONGOING));
		participantRepository.put(MATCH_ID, List.of(
				participant(HOST_ID, MatchParticipantRole.HOST),
				participant(FIRST_GUEST_ID, MatchParticipantRole.GUEST),
				participant(SECOND_GUEST_ID, MatchParticipantRole.GUEST)
		));
		checkInRepository.put(checkIn(HOST_ID));
		checkInRepository.put(checkIn(FIRST_GUEST_ID));
		checkInRepository.put(checkIn(SECOND_GUEST_ID));
	}

	@Test
	void hostReviewsCheckedInGuestWithoutCompletingMeeting() {
		CreateCompanionReviewsResult result = service.create(command(HOST_ID, FIRST_GUEST_ID, keywords()));

		assertEquals(MEETING_ID, result.meetingId());
		assertEquals(1L, result.reviewId());
		assertEquals(CompanionMeetingStatus.ONGOING, result.meetingStatus());
		assertEquals(CompanionMeetingStatus.ONGOING, meetingRepository.findById(MEETING_ID).orElseThrow().status());
		assertEquals(1, reviewRepository.savedReviews.size());
		assertEquals(3, reviewKeywordRepository.savedKeywords.size());
	}

	@Test
	void hostReviewsDifferentGuestsWithSeparateRequests() {
		CreateCompanionReviewsResult first = service.create(command(HOST_ID, FIRST_GUEST_ID, keywords()));
		CreateCompanionReviewsResult second = service.create(command(HOST_ID, SECOND_GUEST_ID, keywords()));

		assertEquals(1L, first.reviewId());
		assertEquals(2L, second.reviewId());
		assertEquals(2, reviewRepository.savedReviews.size());
		assertEquals(6, reviewKeywordRepository.savedKeywords.size());
	}

	@Test
	void guestReviewsHost() {
		CreateCompanionReviewsResult result = service.create(command(FIRST_GUEST_ID, HOST_ID, keywords()));

		assertEquals(1L, result.reviewId());
		assertEquals(HOST_ID, reviewRepository.savedReviews.get(0).revieweeUserId());
	}

	@Test
	void allowsReviewOnCompletedMeetingWithoutChangingStatus() {
		meetingRepository.save(meeting(CompanionMeetingStatus.COMPLETED));

		CreateCompanionReviewsResult result = service.create(command(HOST_ID, FIRST_GUEST_ID, keywords()));

		assertEquals(CompanionMeetingStatus.COMPLETED, result.meetingStatus());
		assertEquals(CompanionMeetingStatus.COMPLETED, meetingRepository.findById(MEETING_ID).orElseThrow().status());
		assertEquals(1, reviewRepository.savedReviews.size());
	}

	@Test
	void rejectsGuestReviewingAnotherGuest() {
		assertThrows(
				InvalidReviewTargetException.class,
				() -> service.create(command(FIRST_GUEST_ID, SECOND_GUEST_ID, keywords()))
		);
	}

	@Test
	void rejectsInvalidKeywordCategoryCount() {
		assertThrows(
				InvalidReviewKeywordCountException.class,
				() -> service.create(command(
						HOST_ID,
						FIRST_GUEST_ID,
						List.of(ReviewKeyword.FAST_RESPONSE, ReviewKeyword.GOOD_MANNERS)
				))
		);
	}

	@Test
	void rejectsRepeatedReviewForSameMeetingReviewerAndReviewee() {
		reviewRepository.put(new CompanionReview(99L, MEETING_ID, HOST_ID, FIRST_GUEST_ID, 5, NOW.minusMinutes(1)));

		assertThrows(
				CompanionReviewAlreadyExistsException.class,
				() -> service.create(command(HOST_ID, FIRST_GUEST_ID, keywords()))
		);
		assertTrue(reviewRepository.savedReviews.isEmpty());
	}

	private CreateCompanionReviewsCommand command(
			final Long reviewerUserId,
			final Long revieweeUserId,
			final List<ReviewKeyword> keywords
	) {
		return new CreateCompanionReviewsCommand(reviewerUserId, MEETING_ID, revieweeUserId, 5, keywords);
	}

	private List<ReviewKeyword> keywords() {
		return List.of(
				ReviewKeyword.FAST_RESPONSE,
				ReviewKeyword.GOOD_MANNERS,
				ReviewKeyword.PUNCTUAL
		);
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

		@Override
		public boolean completeIfOngoing(final Long meetingId, final LocalDateTime completedAt) {
			CompanionMeeting meeting = meetings.get(meetingId);
			if (meeting == null || meeting.status() != CompanionMeetingStatus.ONGOING) {
				return false;
			}
			meetings.put(meetingId, new CompanionMeeting(
					meeting.id(),
					meeting.matchId(),
					CompanionMeetingStatus.COMPLETED,
					meeting.startedAt(),
					completedAt
			));
			return true;
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

	private static final class FakeCompanionReviewRepository implements CompanionReviewRepository {

		private final Map<Long, CompanionReview> reviews = new HashMap<>();
		private final Set<Key> keys = new HashSet<>();
		private final List<CompanionReview> savedReviews = new ArrayList<>();
		private long nextId = 1L;

		@Override
		public CompanionReview save(final CompanionReview model) {
			CompanionReview saved = withId(model);
			reviews.put(saved.id(), saved);
			keys.add(new Key(saved.meetingId(), saved.reviewerUserId(), saved.revieweeUserId()));
			savedReviews.add(saved);
			return saved;
		}

		@Override
		public Optional<CompanionReview> findById(final Long id) {
			return Optional.ofNullable(reviews.get(id));
		}

		@Override
		public boolean existsByMeetingIdAndReviewerUserIdAndRevieweeUserId(
				final Long meetingId,
				final Long reviewerUserId,
				final Long revieweeUserId
		) {
			return keys.contains(new Key(meetingId, reviewerUserId, revieweeUserId));
		}

		private void put(final CompanionReview review) {
			reviews.put(review.id(), review);
			keys.add(new Key(review.meetingId(), review.reviewerUserId(), review.revieweeUserId()));
		}

		private CompanionReview withId(final CompanionReview model) {
			if (model.id() != null) {
				return model;
			}
			return new CompanionReview(
					nextId++,
					model.meetingId(),
					model.reviewerUserId(),
					model.revieweeUserId(),
					model.rating(),
					model.createdAt()
			);
		}

		private record Key(Long meetingId, Long reviewerUserId, Long revieweeUserId) {
		}
	}

	private static final class FakeCompanionReviewKeywordRepository implements CompanionReviewKeywordRepository {

		private final List<CompanionReviewKeyword> savedKeywords = new ArrayList<>();

		@Override
		public CompanionReviewKeyword save(final CompanionReviewKeyword model) {
			savedKeywords.add(model);
			return model;
		}

		@Override
		public Optional<CompanionReviewKeyword> findById(final CompanionReviewKeyword.Key key) {
			return savedKeywords.stream()
					.filter(keyword -> keyword.key().equals(key))
					.findFirst();
		}
	}
}
