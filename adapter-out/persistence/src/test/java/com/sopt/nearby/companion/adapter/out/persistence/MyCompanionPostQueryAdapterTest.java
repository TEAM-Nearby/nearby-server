// 내가 작성한 동행 모집글 목록 조회 어댑터의 필터와 조인 매핑을 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewKeywordEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionApplicationJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewKeywordJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MyCompanionPostQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.post.MyCompanionPostSummary;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import java.math.BigDecimal;
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
class MyCompanionPostQueryAdapterTest {

	private static final Long HOST_USER_ID = 1L;
	private static final Long OTHER_HOST_USER_ID = 9L;
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 18, 0);

	@Autowired
	private CompanionPostJpaRepository postJpaRepository;

	@Autowired
	private CompanionApplicationJpaRepository applicationJpaRepository;

	@Autowired
	private CompanionMatchJpaRepository matchJpaRepository;

	@Autowired
	private CompanionMeetingJpaRepository meetingJpaRepository;

	@Autowired
	private CompanionScheduleJpaRepository scheduleJpaRepository;

	@Autowired
	private CompanionReviewJpaRepository reviewJpaRepository;

	@Autowired
	private CompanionReviewKeywordJpaRepository reviewKeywordJpaRepository;

	@Autowired
	private PlaceCacheJpaRepository placeCacheJpaRepository;

	@Autowired
	private MyCompanionPostQueryJpaRepository queryJpaRepository;

	@Test
	void findsOnlyMyPostsWithScheduleParticipantsAndReviewKeywords() {
		MyCompanionPostQueryAdapter adapter = new MyCompanionPostQueryAdapter(queryJpaRepository);

		PlaceCacheEntity firstPlace = placeCacheJpaRepository.saveAndFlush(place(
				"first-place",
				"시우다드 콘달",
				"바르셀로나 Rambla de Catalunya, 16",
				"https://map.image.url/map.png"
		));
		PlaceCacheEntity secondPlace = placeCacheJpaRepository.saveAndFlush(place(
				"second-place",
				"이태원",
				"서울 용산구 이태원로",
				null
		));

		CompanionPostEntity olderPost = postJpaRepository.saveAndFlush(post(
				HOST_USER_ID,
				firstPlace.getId(),
				CompanionPostStatus.CLOSED,
				NOW.minusDays(2),
				"확정된 동행 모집글"
		));
		CompanionPostEntity newerPost = postJpaRepository.saveAndFlush(post(
				HOST_USER_ID,
				secondPlace.getId(),
				CompanionPostStatus.RECRUITING,
				NOW.minusDays(1),
				"아직 확정되지 않은 모집글"
		));
		postJpaRepository.saveAndFlush(post(
				OTHER_HOST_USER_ID,
				secondPlace.getId(),
				CompanionPostStatus.RECRUITING,
				NOW,
				"다른 사용자의 모집글"
		));
		postJpaRepository.saveAndFlush(post(
				HOST_USER_ID,
				secondPlace.getId(),
				CompanionPostStatus.CANCELED,
				NOW.plusHours(1),
				"취소된 모집글"
		));

		applicationJpaRepository.saveAndFlush(application(olderPost.getId(), 2L, CompanionApplicationStatus.ACCEPTED, NOW));
		applicationJpaRepository.saveAndFlush(application(olderPost.getId(), 3L, CompanionApplicationStatus.ACCEPTED, NOW));
		applicationJpaRepository.saveAndFlush(application(olderPost.getId(), 3L, CompanionApplicationStatus.CANCELED, NOW.plusMinutes(1)));

		CompanionMatchEntity match = matchJpaRepository.saveAndFlush(new CompanionMatchEntity(
				null,
				olderPost.getId(),
				CompanionMatchStatus.COMPLETED,
				NOW.minusDays(1)
		));
		CompanionMeetingEntity meeting = meetingJpaRepository.saveAndFlush(new CompanionMeetingEntity(
				null,
				match.getId(),
				CompanionMeetingStatus.COMPLETED,
				NOW.minusHours(1),
				NOW
		));
		scheduleJpaRepository.saveAndFlush(new CompanionScheduleEntity(
				null,
				match.getId(),
				firstPlace.getId(),
				LocalDateTime.of(2026, 6, 29, 19, 0),
				null,
				true
		));
		CompanionReviewEntity review = reviewJpaRepository.saveAndFlush(new CompanionReviewEntity(
				null,
				meeting.getId(),
				2L,
				HOST_USER_ID,
				5,
				NOW
		));
		reviewKeywordJpaRepository.saveAndFlush(new CompanionReviewKeywordEntity(review.getId(), ReviewKeyword.PUNCTUAL));
		reviewKeywordJpaRepository.saveAndFlush(new CompanionReviewKeywordEntity(review.getId(), ReviewKeyword.GOOD_MANNERS));

		List<MyCompanionPostSummary> result = adapter.findAllByHostUserId(HOST_USER_ID);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).postId()).isEqualTo(newerPost.getId());
		assertThat(result.get(0).scheduledAt()).isNull();
		assertThat(result.get(0).currentParticipants()).isEqualTo(1);
		assertThat(result.get(0).reviewKeywords()).isEmpty();

		assertThat(result.get(1).postId()).isEqualTo(olderPost.getId());
		assertThat(result.get(1).scheduledAt()).isEqualTo(LocalDateTime.of(2026, 6, 29, 19, 0));
		assertThat(result.get(1).place().googlePlaceId()).isEqualTo("first-place");
		assertThat(result.get(1).place().name()).isEqualTo("시우다드 콘달");
		assertThat(result.get(1).place().address()).isEqualTo("바르셀로나 Rambla de Catalunya, 16");
		assertThat(result.get(1).place().latitude()).isEqualByComparingTo(new BigDecimal("41.39020500"));
		assertThat(result.get(1).place().longitude()).isEqualByComparingTo(new BigDecimal("2.16354800"));
		assertThat(result.get(1).currentParticipants()).isEqualTo(2);
		assertThat(result.get(1).maxParticipants()).isEqualTo(4);
		assertThat(result.get(1).content()).isEqualTo("확정된 동행 모집글");
		assertThat(result.get(1).reviewKeywords()).containsExactly(ReviewKeyword.GOOD_MANNERS, ReviewKeyword.PUNCTUAL);
	}

	private PlaceCacheEntity place(
			final String googlePlaceId,
			final String name,
			final String address,
			final String photoReference
	) {
		return new PlaceCacheEntity(
				null,
				googlePlaceId,
				name,
				address,
				new BigDecimal("41.39020500"),
				new BigDecimal("2.16354800"),
				"restaurant",
				null,
				new BigDecimal("4.50"),
				10,
				photoReference,
				PlaceBusinessStatus.OPERATIONAL
		);
	}

	private CompanionPostEntity post(
			final Long hostUserId,
			final Long placeId,
			final CompanionPostStatus status,
			final LocalDateTime createdAt,
			final String content
	) {
		return new CompanionPostEntity(
				null,
				hostUserId,
				placeId,
				LocalDateTime.of(2026, 6, 29, 19, 0),
				4,
				content,
				"https://openchat.example",
				status,
				createdAt
		);
	}

	private CompanionApplicationEntity application(
			final Long postId,
			final Long applicantUserId,
			final CompanionApplicationStatus status,
			final LocalDateTime createdAt
	) {
		return new CompanionApplicationEntity(
				null,
				postId,
				applicantUserId,
				status,
				null,
				createdAt
		);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = {
			CompanionApplicationEntity.class,
			CompanionPostEntity.class,
			CompanionMatchEntity.class,
			CompanionMeetingEntity.class,
			CompanionScheduleEntity.class,
			CompanionReviewEntity.class,
			CompanionReviewKeywordEntity.class,
			PlaceCacheEntity.class
	})
	@EnableJpaRepositories(basePackageClasses = {
			CompanionApplicationJpaRepository.class,
			CompanionPostJpaRepository.class,
			CompanionMatchJpaRepository.class,
			CompanionMeetingJpaRepository.class,
			CompanionScheduleJpaRepository.class,
			CompanionReviewJpaRepository.class,
			CompanionReviewKeywordJpaRepository.class,
			MyCompanionPostQueryJpaRepository.class,
			PlaceCacheJpaRepository.class
	})
	static class TestApplication {
	}
}
