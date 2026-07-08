// 동행 후기 저장소 어댑터의 저장, 조회, 반복 등록 방지를 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewJpaRepository;
import com.sopt.nearby.companion.domain.exception.CompanionReviewAlreadyExistsException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.review.CompanionReview;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionReviewRepositoryAdapterTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 18, 35);

	@Autowired
	private CompanionPostJpaRepository postJpaRepository;

	@Autowired
	private CompanionMatchJpaRepository matchJpaRepository;

	@Autowired
	private CompanionMeetingJpaRepository meetingJpaRepository;

	@Autowired
	private CompanionReviewJpaRepository reviewJpaRepository;

	@Test
	void savesReviewAndChecksExistingReviewerRevieweePair() {
		CompanionReviewRepositoryAdapter adapter = new CompanionReviewRepositoryAdapter(reviewJpaRepository);
		CompanionMeetingEntity meeting = meetingJpaRepository.saveAndFlush(meeting(match().getId()));

		CompanionReview saved = adapter.save(review(meeting.getId(), 1L, 2L));

		assertThat(saved.id()).isNotNull();
		assertThat(adapter.findById(saved.id())).isPresent();
		assertThat(adapter.existsByMeetingIdAndReviewerUserIdAndRevieweeUserId(meeting.getId(), 1L, 2L)).isTrue();
	}

	@Test
	void throwsAlreadyExistsWhenSameMeetingReviewerRevieweePairIsSavedAgain() {
		CompanionReviewRepositoryAdapter adapter = new CompanionReviewRepositoryAdapter(reviewJpaRepository);
		CompanionMeetingEntity meeting = meetingJpaRepository.saveAndFlush(meeting(match().getId()));
		adapter.save(review(meeting.getId(), 1L, 2L));

		assertThatThrownBy(() -> adapter.save(review(meeting.getId(), 1L, 2L)))
				.isInstanceOf(CompanionReviewAlreadyExistsException.class);
	}

	private CompanionMatchEntity match() {
		CompanionPostEntity post = postJpaRepository.saveAndFlush(new CompanionPostEntity(
				null,
				1L,
				30L,
				NOW,
				4,
				"함께 밥 먹을 동행을 구해요.",
				"https://openchat.example",
				CompanionPostStatus.CLOSED,
				NOW.minusDays(1)
		));
		return matchJpaRepository.saveAndFlush(new CompanionMatchEntity(
				null,
				post.getId(),
				CompanionMatchStatus.SCHEDULE_CONFIRMED,
				NOW.minusDays(1)
		));
	}

	private CompanionMeetingEntity meeting(final Long matchId) {
		return new CompanionMeetingEntity(null, matchId, CompanionMeetingStatus.ONGOING, NOW.minusMinutes(5), null);
	}

	private CompanionReview review(final Long meetingId, final Long reviewerUserId, final Long revieweeUserId) {
		return new CompanionReview(null, meetingId, reviewerUserId, revieweeUserId, 5, NOW);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = {
			CompanionPostEntity.class,
			CompanionMatchEntity.class,
			CompanionMeetingEntity.class,
			CompanionReviewEntity.class
	})
	@EnableJpaRepositories(basePackageClasses = {
			CompanionPostJpaRepository.class,
			CompanionMatchJpaRepository.class,
			CompanionMeetingJpaRepository.class,
			CompanionReviewJpaRepository.class
	})
	static class TestApplication {
	}
}
