// 동행 후기 등록 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CannotReviewSelfException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReviewAlreadyExistsException;
import com.sopt.nearby.companion.domain.exception.CompanionReviewMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CurrentUserNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionReviewException;
import com.sopt.nearby.companion.domain.exception.InvalidReviewKeywordCountException;
import com.sopt.nearby.companion.domain.exception.InvalidReviewKeywordException;
import com.sopt.nearby.companion.domain.exception.InvalidReviewRatingException;
import com.sopt.nearby.companion.domain.exception.InvalidReviewRequestException;
import com.sopt.nearby.companion.domain.exception.InvalidReviewTargetException;
import com.sopt.nearby.companion.domain.exception.RevieweeNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.RevieweeNotFoundException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.review.CompanionReview;
import com.sopt.nearby.companion.domain.model.review.CompanionReviewKeyword;
import com.sopt.nearby.companion.domain.model.review.CompanionReviewKeywordCategory;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.port.in.CreateCompanionReviewsUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.CompanionReviewKeywordRepository;
import com.sopt.nearby.companion.port.out.CompanionReviewRepository;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class CreateCompanionReviewsService implements CreateCompanionReviewsUseCase {

	private static final int MIN_RATING = 1;
	private static final int MAX_RATING = 5;
	private static final int MIN_CONSIDERATION_KEYWORD_COUNT = 1;
	private static final int MAX_CONSIDERATION_KEYWORD_COUNT = 3;
	private static final int REQUIRED_TIME_PROMISE_KEYWORD_COUNT = 1;

	private final CompanionMeetingRepository meetingRepository;
	private final CompanionMatchParticipantRepository participantRepository;
	private final MeetingCheckInRepository checkInRepository;
	private final CompanionReviewRepository reviewRepository;
	private final CompanionReviewKeywordRepository reviewKeywordRepository;
	private final Clock clock;

	public CreateCompanionReviewsService(
			final CompanionMeetingRepository meetingRepository,
			final CompanionMatchParticipantRepository participantRepository,
			final MeetingCheckInRepository checkInRepository,
			final CompanionReviewRepository reviewRepository,
			final CompanionReviewKeywordRepository reviewKeywordRepository,
			final Clock clock
	) {
		this.meetingRepository = meetingRepository;
		this.participantRepository = participantRepository;
		this.checkInRepository = checkInRepository;
		this.reviewRepository = reviewRepository;
		this.reviewKeywordRepository = reviewKeywordRepository;
		this.clock = clock;
	}

	@Override
	@Transactional
	public CreateCompanionReviewsResult create(final CreateCompanionReviewsCommand command) {
		validateCommand(command);

		Long revieweeUserId = revieweeUserId(command.revieweeUserId());
		List<ReviewKeyword> keywords = keywords(command.keywords());

		CompanionMeeting meeting = meetingRepository.findById(command.meetingId())
				.orElseThrow(CompanionMeetingNotFoundException::new);
		validateMeeting(meeting);

		List<CompanionMatchParticipant> participants = participantRepository.findAllByMatchId(meeting.matchId());
		CompanionMatchParticipant reviewer = participants.stream()
				.filter(participant -> participant.userId().equals(command.reviewerUserId()))
				.findFirst()
				.orElseThrow(ForbiddenCompanionReviewException::new);
		CompanionMatchParticipant reviewee = participants.stream()
				.filter(participant -> participant.userId().equals(revieweeUserId))
				.findFirst()
				.orElseThrow(RevieweeNotFoundException::new);

		validateTarget(command.reviewerUserId(), reviewer, reviewee);
		validateCheckIns(meeting.id(), command.reviewerUserId(), revieweeUserId);
		validateReviewNotExists(meeting.id(), command.reviewerUserId(), revieweeUserId);

		LocalDateTime now = LocalDateTime.now(clock);
		Long reviewId = saveReview(
				meeting.id(),
				command.reviewerUserId(),
				revieweeUserId,
				command.rating(),
				keywords,
				now
		);

		return new CreateCompanionReviewsResult(
				meeting.id(),
				reviewId,
				meeting.status()
		);
	}

	private void validateCommand(final CreateCompanionReviewsCommand command) {
		if (command == null || command.reviewerUserId() == null
				|| command.reviewerUserId() <= 0
				|| command.meetingId() == null || command.meetingId() <= 0) {
			throw new InvalidReviewRequestException();
		}
		if (command.rating() < MIN_RATING || command.rating() > MAX_RATING) {
			throw new InvalidReviewRatingException();
		}
	}

	private Long revieweeUserId(final Long value) {
		if (value == null || value <= 0) {
			throw new InvalidReviewTargetException();
		}
		return value;
	}

	private List<ReviewKeyword> keywords(final List<ReviewKeyword> values) {
		if (values == null || values.isEmpty() || values.stream().anyMatch(keyword -> keyword == null)) {
			throw new InvalidReviewKeywordException();
		}
		List<ReviewKeyword> distinct = List.copyOf(new LinkedHashSet<>(values));
		if (distinct.size() != values.size()) {
			throw new InvalidReviewKeywordCountException();
		}
		long considerationCount = countByCategory(distinct, CompanionReviewKeywordCategory.CONSIDERATION_COMMUNICATION);
		long timePromiseCount = countByCategory(distinct, CompanionReviewKeywordCategory.TIME_PROMISE);
		if (considerationCount < MIN_CONSIDERATION_KEYWORD_COUNT
				|| considerationCount > MAX_CONSIDERATION_KEYWORD_COUNT
				|| timePromiseCount != REQUIRED_TIME_PROMISE_KEYWORD_COUNT) {
			throw new InvalidReviewKeywordCountException();
		}
		return distinct;
	}

	private long countByCategory(
			final List<ReviewKeyword> keywords,
			final CompanionReviewKeywordCategory category
	) {
		return keywords.stream()
				.filter(keyword -> keyword.getCategory() == category)
				.count();
	}

	private void validateMeeting(final CompanionMeeting meeting) {
		if (meeting.status() == CompanionMeetingStatus.CANCELED) {
			throw new CompanionReviewMeetingAlreadyCanceledException();
		}
	}

	private void validateTarget(
			final Long reviewerUserId,
			final CompanionMatchParticipant reviewer,
			final CompanionMatchParticipant reviewee
	) {
		if (reviewee.userId().equals(reviewerUserId)) {
			throw new CannotReviewSelfException();
		}
		boolean allowedReviewTarget = (reviewer.role() == MatchParticipantRole.HOST
				&& reviewee.role() == MatchParticipantRole.GUEST)
				|| (reviewer.role() == MatchParticipantRole.GUEST
				&& reviewee.role() == MatchParticipantRole.HOST);
		if (!allowedReviewTarget) {
			throw new InvalidReviewTargetException();
		}
	}

	private void validateCheckIns(
			final Long meetingId,
			final Long reviewerUserId,
			final Long revieweeUserId
	) {
		if (checkInRepository.findByMeetingIdAndUserId(meetingId, reviewerUserId).isEmpty()) {
			throw new CurrentUserNotCheckedInException();
		}
		if (checkInRepository.findByMeetingIdAndUserId(meetingId, revieweeUserId).isEmpty()) {
			throw new RevieweeNotCheckedInException();
		}
	}

	private void validateReviewNotExists(
			final Long meetingId,
			final Long reviewerUserId,
			final Long revieweeUserId
	) {
		if (reviewRepository.existsByMeetingIdAndReviewerUserIdAndRevieweeUserId(
				meetingId,
				reviewerUserId,
				revieweeUserId
		)) {
			throw new CompanionReviewAlreadyExistsException();
		}
	}

	private Long saveReview(
			final Long meetingId,
			final Long reviewerUserId,
			final Long revieweeUserId,
			final int rating,
			final List<ReviewKeyword> keywords,
			final LocalDateTime now
	) {
		CompanionReview savedReview = reviewRepository.save(new CompanionReview(
				null,
				meetingId,
				reviewerUserId,
				revieweeUserId,
				rating,
				now
		));
		keywords.forEach(keyword -> reviewKeywordRepository.save(
				new CompanionReviewKeyword(savedReview.id(), keyword)
		));
		return savedReview.id();
	}
}
