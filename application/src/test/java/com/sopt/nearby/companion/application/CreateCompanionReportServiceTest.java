// 동행 신고 메일의 커밋 이후 발송을 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.report.CompanionReport;
import com.sopt.nearby.companion.domain.model.report.CompanionReportReason;
import com.sopt.nearby.companion.domain.model.report.ReportReason;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.CompanionReportMail;
import com.sopt.nearby.companion.port.out.CompanionReportMailContext;
import com.sopt.nearby.companion.port.out.CompanionReportMailContextQueryPort;
import com.sopt.nearby.companion.port.out.CompanionReportMailSender;
import com.sopt.nearby.companion.port.out.CompanionReportReasonRepository;
import com.sopt.nearby.companion.port.out.CompanionReportRepository;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class CreateCompanionReportServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T00:00:00Z"), ZoneOffset.UTC);
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 2, 0, 0);

	@Test
	void sendsMailOnlyAfterTransactionCommit() {
		CapturingMailSender mailSender = new CapturingMailSender();
		CreateCompanionReportService service = new CreateCompanionReportService(
				new MeetingRepository(),
				new ParticipantRepository(),
				new CheckInRepository(),
				new ReportRepository(),
				new ReportReasonRepository(),
				new MailContextQueryPort(),
				mailSender,
				CLOCK
		);

		TransactionSynchronizationManager.initSynchronization();
		try {
			service.create(new CreateCompanionReportCommand(
					10L,
					1L,
					11L,
					List.of(ReportReason.BAD_ATTITUDE),
					"상세 내용"
			));

			assertTrue(mailSender.sent.isEmpty());
			assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
			TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();
			assertEquals(1, mailSender.sent.size());
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	private static final class MeetingRepository implements CompanionMeetingRepository {
		@Override
		public CompanionMeeting save(final CompanionMeeting model) {
			return model;
		}

		@Override
		public Optional<CompanionMeeting> findById(final Long id) {
			return Optional.of(new CompanionMeeting(1L, 10L, CompanionMeetingStatus.ONGOING, NOW, null));
		}

		@Override
		public boolean completeIfOngoing(final Long meetingId, final LocalDateTime completedAt) {
			return false;
		}
	}

	private static final class ParticipantRepository implements CompanionMatchParticipantRepository {
		@Override
		public CompanionMatchParticipant save(final CompanionMatchParticipant model) {
			return model;
		}

		@Override
		public Optional<CompanionMatchParticipant> findById(final Long id) {
			return Optional.empty();
		}

		@Override
		public List<CompanionMatchParticipant> findAllByMatchId(final Long matchId) {
			return List.of(
					new CompanionMatchParticipant(1L, 10L, 10L, null, MatchParticipantRole.HOST),
					new CompanionMatchParticipant(2L, 10L, 11L, null, MatchParticipantRole.GUEST)
			);
		}

		@Override
		public boolean existsByMatchIdAndUserId(final Long matchId, final Long userId) {
			return true;
		}
	}

	private static final class CheckInRepository implements MeetingCheckInRepository {
		@Override
		public MeetingCheckIn save(final MeetingCheckIn model) {
			return model;
		}

		@Override
		public Optional<MeetingCheckIn> findById(final Long id) {
			return Optional.empty();
		}

		@Override
		public Optional<MeetingCheckIn> findByMeetingIdAndUserId(final Long meetingId, final Long userId) {
			return Optional.of(new MeetingCheckIn(1L, meetingId, userId, null, null, NOW, null));
		}

		@Override
		public long countByMeetingId(final Long meetingId) {
			return 1L;
		}

		@Override
		public long countCompletedByMeetingId(final Long meetingId) {
			return 0L;
		}

		@Override
		public MeetingCheckIn saveIfAbsent(final MeetingCheckIn checkIn) {
			return checkIn;
		}
	}

	private static final class ReportRepository implements CompanionReportRepository {
		@Override
		public CompanionReport save(final CompanionReport model) {
			return new CompanionReport(1L, model.meetingId(), model.reporterUserId(), model.reportedUserId(),
					model.detail(), model.createdAt());
		}

		@Override
		public Optional<CompanionReport> findById(final Long id) {
			return Optional.empty();
		}

		@Override
		public boolean existsByMeetingIdAndReporterUserIdAndReportedUserId(
				final Long meetingId,
				final Long reporterUserId,
				final Long reportedUserId
		) {
			return false;
		}
	}

	private static final class ReportReasonRepository implements CompanionReportReasonRepository {
		@Override
		public CompanionReportReason save(final CompanionReportReason model) {
			return model;
		}

		@Override
		public Optional<CompanionReportReason> findById(final CompanionReportReason.Key key) {
			return Optional.empty();
		}
	}

	private static final class MailContextQueryPort implements CompanionReportMailContextQueryPort {
		@Override
		public Optional<CompanionReportMailContext> findByMeetingIdAndUserIds(
				final Long meetingId,
				final Long reporterUserId,
				final Long reportedUserId
		) {
			return Optional.of(new CompanionReportMailContext(
					"신고자", null, "피신고자", null, 20L, "동행 본문", NOW,
					CompanionPostMeetingTimeType.SCHEDULED, "Paris, France"
			));
		}
	}

	private static final class CapturingMailSender implements CompanionReportMailSender {
		private final List<CompanionReportMail> sent = new ArrayList<>();

		@Override
		public void send(final CompanionReportMail mail) {
			sent.add(mail);
		}
	}
}
