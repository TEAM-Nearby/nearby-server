// 동행 신고 저장소 어댑터의 저장과 중복 신고 예외 변환을 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReportEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReportJpaRepository;
import com.sopt.nearby.companion.domain.exception.CompanionReportAlreadyExistsException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.report.CompanionReport;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionReportRepositoryAdapterTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 2, 12, 0);

	@Autowired
	private CompanionPostJpaRepository postJpaRepository;

	@Autowired
	private CompanionMatchJpaRepository matchJpaRepository;

	@Autowired
	private CompanionMeetingJpaRepository meetingJpaRepository;

	@Autowired
	private CompanionReportJpaRepository reportJpaRepository;

	@Test
	void savesReportAndChecksExistingReporterReportedPair() {
		CompanionReportRepositoryAdapter adapter = new CompanionReportRepositoryAdapter(reportJpaRepository);
		CompanionMeetingEntity meeting = meeting();

		CompanionReport saved = adapter.save(report(meeting.getId()));

		assertThat(saved.id()).isNotNull();
		assertThat(adapter.findById(saved.id())).isPresent();
		assertThat(adapter.existsByMeetingIdAndReporterUserIdAndReportedUserId(
				meeting.getId(), 10L, 11L
		)).isTrue();
	}

	@Test
	void translatesConcurrentDuplicateReportToAlreadyExistsException() {
		CompanionReportRepositoryAdapter adapter = new CompanionReportRepositoryAdapter(reportJpaRepository);
		CompanionMeetingEntity meeting = meeting();
		adapter.save(report(meeting.getId()));

		assertThatThrownBy(() -> adapter.save(report(meeting.getId())))
				.isInstanceOf(CompanionReportAlreadyExistsException.class);
	}

	@Test
	void rethrowsDataIntegrityViolationWhenMeetingDoesNotExist() {
		CompanionReportRepositoryAdapter adapter = new CompanionReportRepositoryAdapter(reportJpaRepository);

		assertThatThrownBy(() -> adapter.save(report(999L)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private CompanionMeetingEntity meeting() {
		CompanionPostEntity post = postJpaRepository.saveAndFlush(new CompanionPostEntity(
				null,
				10L,
				30L,
				NOW,
				2,
				"파리 동행을 구해요.",
				"https://openchat.example",
				CompanionPostStatus.CLOSED,
				NOW.minusDays(1)
		));
		CompanionMatchEntity match = matchJpaRepository.saveAndFlush(new CompanionMatchEntity(
				null,
				post.getId(),
				CompanionMatchStatus.SCHEDULE_CONFIRMED,
				NOW.minusDays(1)
		));
		return meetingJpaRepository.saveAndFlush(new CompanionMeetingEntity(
				null,
				match.getId(),
				CompanionMeetingStatus.ONGOING,
				NOW.minusMinutes(5),
				null
		));
	}

	private CompanionReport report(final Long meetingId) {
		return new CompanionReport(null, meetingId, 10L, 11L, "상세 내용", NOW);
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = {
			CompanionPostEntity.class,
			CompanionMatchEntity.class,
			CompanionMeetingEntity.class,
			CompanionReportEntity.class
	})
	@EnableJpaRepositories(basePackageClasses = {
			CompanionPostJpaRepository.class,
			CompanionMatchJpaRepository.class,
			CompanionMeetingJpaRepository.class,
			CompanionReportJpaRepository.class
	})
	static class TestApplication {
	}
}
