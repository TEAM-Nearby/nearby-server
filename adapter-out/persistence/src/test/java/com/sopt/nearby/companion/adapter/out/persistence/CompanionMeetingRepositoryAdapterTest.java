// 동행 만남 저장소 어댑터의 조건부 완료 상태 변경을 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionMeetingRepositoryAdapterTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 19, 0);

	@Autowired
	private CompanionPostJpaRepository companionPostJpaRepository;

	@Autowired
	private CompanionMatchJpaRepository companionMatchJpaRepository;

	@Autowired
	private CompanionMeetingJpaRepository companionMeetingJpaRepository;

	@Test
	void completesOnlyOngoingMeeting() {
		CompanionMeetingRepositoryAdapter adapter =
				new CompanionMeetingRepositoryAdapter(companionMeetingJpaRepository);
		CompanionPostEntity post = companionPostJpaRepository.saveAndFlush(post());
		CompanionMatchEntity match = companionMatchJpaRepository.saveAndFlush(new CompanionMatchEntity(
				null,
				post.getId(),
				CompanionMatchStatus.SCHEDULE_CONFIRMED,
				NOW.minusDays(1)
		));
		CompanionMeetingEntity meeting = companionMeetingJpaRepository.saveAndFlush(new CompanionMeetingEntity(
				null,
				match.getId(),
				CompanionMeetingStatus.ONGOING,
				NOW.minusHours(1),
				null
		));

		assertThat(adapter.completeIfOngoing(meeting.getId(), NOW)).isTrue();
		assertThat(companionMeetingJpaRepository.findById(meeting.getId()))
				.get()
				.satisfies(completed -> {
					assertThat(completed.getStatus()).isEqualTo(CompanionMeetingStatus.COMPLETED);
					assertThat(completed.getCompletedAt()).isEqualTo(NOW);
				});

		assertThat(adapter.completeIfOngoing(meeting.getId(), NOW.plusMinutes(1))).isFalse();
		assertThat(companionMeetingJpaRepository.findById(meeting.getId()))
				.get()
				.extracting(CompanionMeetingEntity::getCompletedAt)
				.isEqualTo(NOW);
	}

	private CompanionPostEntity post() {
		return new CompanionPostEntity(
				null,
				7L,
				30L,
				NOW.plusDays(1),
				4,
				"함께 밥 먹을 동행을 구해요.",
				"https://openchat.example",
				CompanionPostStatus.CLOSED,
				NOW.minusDays(2)
		);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = {
			CompanionPostEntity.class,
			CompanionMatchEntity.class,
			CompanionMeetingEntity.class
	})
	@EnableJpaRepositories(basePackageClasses = {
			CompanionPostJpaRepository.class,
			CompanionMatchJpaRepository.class,
			CompanionMeetingJpaRepository.class
	})
	static class TestApplication {
	}
}
