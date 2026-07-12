// 동행 후기 대상 목록 조회 어댑터의 체크인 필터와 리뷰 작성 여부 매핑을 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchParticipantEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCheckInEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchParticipantJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewTargetQueryJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MeetingCheckInJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.review.CompanionReviewTarget;
import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionReviewTargetQueryAdapterTest {

	private static final Long HOST_USER_ID = 1L;
	private static final Long FIRST_GUEST_ID = 2L;
	private static final Long SECOND_GUEST_ID = 3L;
	private static final LocalDateTime MEETING_AT = LocalDateTime.of(2026, 6, 18, 16, 30);

	@Autowired
	private CompanionPostJpaRepository postJpaRepository;

	@Autowired
	private CompanionMatchJpaRepository matchJpaRepository;

	@Autowired
	private CompanionMatchParticipantJpaRepository participantJpaRepository;

	@Autowired
	private CompanionMeetingJpaRepository meetingJpaRepository;

	@Autowired
	private CompanionScheduleJpaRepository scheduleJpaRepository;

	@Autowired
	private CompanionProfileJpaRepository profileJpaRepository;

	@Autowired
	private MeetingCheckInJpaRepository checkInJpaRepository;

	@Autowired
	private CompanionReviewJpaRepository reviewJpaRepository;

	@Autowired
	private PlaceCacheJpaRepository placeCacheJpaRepository;

	@Autowired
	private CompanionReviewTargetQueryJpaRepository queryJpaRepository;

	@Test
	void hostReadsOnlyCheckedInGuestTargetsWithWrittenReviewState() {
		CompanionReviewTargetQueryAdapter adapter = new CompanionReviewTargetQueryAdapter(queryJpaRepository);
		TestFixture fixture = saveFixture();
		checkInJpaRepository.saveAndFlush(checkIn(fixture.meeting().getId(), HOST_USER_ID));
		checkInJpaRepository.saveAndFlush(checkIn(fixture.meeting().getId(), FIRST_GUEST_ID));
		reviewJpaRepository.saveAndFlush(review(fixture.meeting().getId(), HOST_USER_ID, FIRST_GUEST_ID));

		List<CompanionReviewTarget> result = adapter.findAllByMeetingIdAndReviewerUserIdAndTargetRole(
				fixture.meeting().getId(),
				HOST_USER_ID,
				MatchParticipantRole.GUEST
		);

		assertThat(result).hasSize(1);
		CompanionReviewTarget target = result.get(0);
		assertThat(target.revieweeUserId()).isEqualTo(FIRST_GUEST_ID);
		assertThat(target.nickname()).isEqualTo("조예원");
		assertThat(target.profileImageUrl()).isEqualTo("https://image.url/profile-2.png");
		assertThat(target.cityName()).isEqualTo("바르셀로나");
		assertThat(target.meetingDate()).isEqualTo(LocalDate.of(2026, 6, 18));
		assertThat(target.checkedIn()).isTrue();
		assertThat(target.hasWrittenReview()).isTrue();
	}

	@Test
	void guestReadsCheckedInHostTarget() {
		CompanionReviewTargetQueryAdapter adapter = new CompanionReviewTargetQueryAdapter(queryJpaRepository);
		TestFixture fixture = saveFixture();
		checkInJpaRepository.saveAndFlush(checkIn(fixture.meeting().getId(), HOST_USER_ID));
		checkInJpaRepository.saveAndFlush(checkIn(fixture.meeting().getId(), FIRST_GUEST_ID));

		List<CompanionReviewTarget> result = adapter.findAllByMeetingIdAndReviewerUserIdAndTargetRole(
				fixture.meeting().getId(),
				FIRST_GUEST_ID,
				MatchParticipantRole.HOST
		);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).revieweeUserId()).isEqualTo(HOST_USER_ID);
		assertThat(result.get(0).nickname()).isEqualTo("정지영");
		assertThat(result.get(0).hasWrittenReview()).isFalse();
	}

	private TestFixture saveFixture() {
		PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place());
		profileJpaRepository.saveAndFlush(profile(HOST_USER_ID, "정지영", UserGender.FEMALE));
		profileJpaRepository.saveAndFlush(profile(FIRST_GUEST_ID, "조예원", UserGender.FEMALE));
		profileJpaRepository.saveAndFlush(profile(SECOND_GUEST_ID, "김솝트", UserGender.MALE));
		CompanionPostEntity post = postJpaRepository.saveAndFlush(post(place.getId()));
		CompanionMatchEntity match = matchJpaRepository.saveAndFlush(match(post.getId()));
		participantJpaRepository.saveAndFlush(participant(match.getId(), HOST_USER_ID, MatchParticipantRole.HOST));
		participantJpaRepository.saveAndFlush(participant(match.getId(), FIRST_GUEST_ID, MatchParticipantRole.GUEST));
		participantJpaRepository.saveAndFlush(participant(match.getId(), SECOND_GUEST_ID, MatchParticipantRole.GUEST));
		CompanionMeetingEntity meeting = meetingJpaRepository.saveAndFlush(meeting(match.getId()));
		scheduleJpaRepository.saveAndFlush(schedule(match.getId(), place.getId()));
		return new TestFixture(meeting);
	}

	private PlaceCacheEntity place() {
		return new PlaceCacheEntity(
				null,
				"google-place-id",
				"시우다드 콘달",
				"바르셀로나 Rambla de Catalunya, 16",
				new BigDecimal("41.39020500"),
				new BigDecimal("2.16354800"),
				"restaurant",
				null,
				new BigDecimal("4.50"),
				10,
				null,
				PlaceBusinessStatus.OPERATIONAL
		);
	}

	private CompanionProfileEntity profile(final Long userId, final String nickname, final UserGender gender) {
		return new CompanionProfileEntity(
				null,
				userId,
				nickname,
				gender,
				1998,
				"https://image.url/profile-" + userId + ".png",
				"반갑습니다.",
				new BigDecimal("4.50"),
				3,
				CompanionProfileStatus.ACTIVE
		);
	}

	private CompanionPostEntity post(final Long placeId) {
		return new CompanionPostEntity(
				null,
				HOST_USER_ID,
				placeId,
				MEETING_AT,
				4,
				"함께 밥 먹을 동행을 구해요.",
				"https://openchat.example",
				CompanionPostStatus.CLOSED,
				MEETING_AT.minusDays(1)
		);
	}

	private CompanionMatchEntity match(final Long postId) {
		return new CompanionMatchEntity(null, postId, CompanionMatchStatus.SCHEDULE_CONFIRMED, MEETING_AT.minusDays(1));
	}

	private CompanionMatchParticipantEntity participant(
			final Long matchId,
			final Long userId,
			final MatchParticipantRole role
	) {
		return new CompanionMatchParticipantEntity(null, matchId, userId, null, role);
	}

	private CompanionMeetingEntity meeting(final Long matchId) {
		return new CompanionMeetingEntity(null, matchId, CompanionMeetingStatus.COMPLETED, MEETING_AT.minusMinutes(5), null);
	}

	private CompanionScheduleEntity schedule(final Long matchId, final Long placeId) {
		return new CompanionScheduleEntity(null, matchId, placeId, MEETING_AT, 120, true);
	}

	private MeetingCheckInEntity checkIn(final Long meetingId, final Long userId) {
		return new MeetingCheckInEntity(
				null,
				meetingId,
				userId,
				new BigDecimal("41.39020500"),
				new BigDecimal("2.16354800"),
				MEETING_AT.plusMinutes(5),
				null
		);
	}

	private CompanionReviewEntity review(final Long meetingId, final Long reviewerUserId, final Long revieweeUserId) {
		return new CompanionReviewEntity(null, meetingId, reviewerUserId, revieweeUserId, 5, MEETING_AT.plusMinutes(10));
	}

	private record TestFixture(CompanionMeetingEntity meeting) {
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = {
			CompanionPostEntity.class,
			CompanionMatchEntity.class,
			CompanionMatchParticipantEntity.class,
			CompanionMeetingEntity.class,
			CompanionScheduleEntity.class,
			CompanionProfileEntity.class,
			MeetingCheckInEntity.class,
			CompanionReviewEntity.class,
			PlaceCacheEntity.class
	})
	@EnableJpaRepositories(basePackageClasses = {
			CompanionPostJpaRepository.class,
			CompanionMatchJpaRepository.class,
			CompanionMatchParticipantJpaRepository.class,
			CompanionMeetingJpaRepository.class,
			CompanionScheduleJpaRepository.class,
			CompanionProfileJpaRepository.class,
			MeetingCheckInJpaRepository.class,
			CompanionReviewJpaRepository.class,
			CompanionReviewTargetQueryJpaRepository.class,
			PlaceCacheJpaRepository.class
	})
	static class TestApplication {
	}
}
