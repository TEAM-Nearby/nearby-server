// 동시 개인 완료 요청이 전체 동행 완료로 수렴하는지 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchParticipantEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCheckInEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchParticipantJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MeetingCheckInJpaRepository;
import com.sopt.nearby.companion.application.CompleteCompanionMeetingResult;
import com.sopt.nearby.companion.application.CompleteCompanionMeetingService;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
class CompleteCompanionMeetingConcurrencyAdapterTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-29T19:00:00Z"), ZoneOffset.UTC);
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 19, 0);
	private static final Long HOST_ID = 1L;
	private static final Long GUEST_ID = 2L;

	@Autowired
	private CompanionPostJpaRepository postJpaRepository;

	@Autowired
	private CompanionMatchJpaRepository matchJpaRepository;

	@Autowired
	private CompanionMatchParticipantJpaRepository participantJpaRepository;

	@Autowired
	private CompanionMeetingJpaRepository meetingJpaRepository;

	@Autowired
	private MeetingCheckInJpaRepository checkInJpaRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private DataSource dataSource;

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void concurrentParticipantsCompleteMeetingAndMatch() throws Exception {
		Ids ids = transaction().execute(status -> saveMeeting());
		CompleteCompanionMeetingService service = service();
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<CompleteCompanionMeetingResult> host = executor.submit(
					() -> completeAfterStart(service, ids.meetingId(), HOST_ID, start)
			);
			Future<CompleteCompanionMeetingResult> guest = executor.submit(
					() -> completeAfterStart(service, ids.meetingId(), GUEST_ID, start)
			);
			start.countDown();

			List<CompleteCompanionMeetingResult> results = List.of(
					host.get(5, TimeUnit.SECONDS),
					guest.get(5, TimeUnit.SECONDS)
			);

			assertThat(results).extracting(CompleteCompanionMeetingResult::meetingStatus)
					.containsExactlyInAnyOrder(CompanionMeetingStatus.ONGOING, CompanionMeetingStatus.COMPLETED);
			transaction().executeWithoutResult(status -> {
				assertThat(meetingJpaRepository.findById(ids.meetingId())).get()
						.extracting(CompanionMeetingEntity::getStatus)
						.isEqualTo(CompanionMeetingStatus.COMPLETED);
				assertThat(matchJpaRepository.findById(ids.matchId())).get()
						.extracting(CompanionMatchEntity::getStatus)
						.isEqualTo(CompanionMatchStatus.COMPLETED);
				assertThat(checkInJpaRepository.countByMeetingIdAndCompletedAtIsNotNull(ids.meetingId()))
						.isEqualTo(2L);
			});
		} finally {
			executor.shutdownNow();
		}
	}

	private CompleteCompanionMeetingResult completeAfterStart(
			final CompleteCompanionMeetingService service,
			final Long meetingId,
			final Long userId,
			final CountDownLatch start
	) throws InterruptedException {
		start.await();
		return transaction().execute(status -> service.complete(meetingId, userId));
	}

	private CompleteCompanionMeetingService service() {
		return new CompleteCompanionMeetingService(
				new CompanionMeetingRepositoryAdapter(meetingJpaRepository),
				new CompanionMatchRepositoryAdapter(matchJpaRepository),
				new CompanionMatchParticipantRepositoryAdapter(participantJpaRepository),
				new MeetingCheckInRepositoryAdapter(checkInJpaRepository, dataSource),
				CLOCK
		);
	}

	private Ids saveMeeting() {
		CompanionPostEntity post = postJpaRepository.saveAndFlush(new CompanionPostEntity(
				null,
				HOST_ID,
				30L,
				NOW.plusDays(1),
				2,
				"함께 밥 먹을 동행을 구해요.",
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
		participantJpaRepository.saveAllAndFlush(List.of(
				new CompanionMatchParticipantEntity(null, match.getId(), HOST_ID, null, MatchParticipantRole.HOST),
				new CompanionMatchParticipantEntity(null, match.getId(), GUEST_ID, null, MatchParticipantRole.GUEST)
		));
		CompanionMeetingEntity meeting = meetingJpaRepository.saveAndFlush(new CompanionMeetingEntity(
				null,
				match.getId(),
				CompanionMeetingStatus.ONGOING,
				NOW.minusHours(1),
				null
		));
		checkInJpaRepository.saveAllAndFlush(List.of(
				checkIn(meeting.getId(), HOST_ID),
				checkIn(meeting.getId(), GUEST_ID)
		));
		return new Ids(match.getId(), meeting.getId());
	}

	private MeetingCheckInEntity checkIn(final Long meetingId, final Long userId) {
		return new MeetingCheckInEntity(
				null,
				meetingId,
				userId,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				NOW.minusMinutes(10),
				null
		);
	}

	private TransactionTemplate transaction() {
		return new TransactionTemplate(transactionManager);
	}

	private record Ids(Long matchId, Long meetingId) {
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@EntityScan(basePackageClasses = {
			CompanionPostEntity.class,
			CompanionMatchEntity.class,
			CompanionMatchParticipantEntity.class,
			CompanionMeetingEntity.class,
			MeetingCheckInEntity.class
	})
	@EnableJpaRepositories(basePackageClasses = {
			CompanionPostJpaRepository.class,
			CompanionMatchJpaRepository.class,
			CompanionMatchParticipantJpaRepository.class,
			CompanionMeetingJpaRepository.class,
			MeetingCheckInJpaRepository.class
	})
	static class TestApplication {
	}
}
