// 매칭 기준 신고 자격과 중복 방지 및 커밋 이후 메일 발송을 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sopt.nearby.companion.domain.exception.CompanionReportTargetNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReportAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionReportAlreadyExistsException;
import com.sopt.nearby.companion.domain.exception.CompanionReportSelfNotAllowedException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionReportException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionReportRequestException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.report.CompanionReport;
import com.sopt.nearby.companion.domain.model.report.CompanionReportReason;
import com.sopt.nearby.companion.domain.model.report.ReportReason;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.companion.port.out.CompanionReportMail;
import com.sopt.nearby.companion.port.out.CompanionReportMailContext;
import com.sopt.nearby.companion.port.out.CompanionReportMailContextQueryPort;
import com.sopt.nearby.companion.port.out.CompanionReportMailSender;
import com.sopt.nearby.companion.port.out.CompanionReportReasonRepository;
import com.sopt.nearby.companion.port.out.CompanionReportRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import java.math.BigDecimal;
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
		TestSetup setup = new TestSetup();

		TransactionSynchronizationManager.initSynchronization();
		try {
			setup.service.create(new CreateCompanionReportCommand(
					10L,
					1L,
					11L,
					List.of(ReportReason.BAD_ATTITUDE),
					"상세 내용"
			));

			assertTrue(setup.mailSender.sent.isEmpty());
			assertEquals(1, TransactionSynchronizationManager.getSynchronizations().size());
			TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit();
			assertEquals(1, setup.mailSender.sent.size());
			assertEquals(1, setup.matchRepository.findByIdForUpdateCalls);
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void rejectsNonParticipantReporter() {
		TestSetup setup = new TestSetup();
		setup.participantRepository.participants = List.of(
				new CompanionMatchParticipant(2L, 10L, 11L, null, MatchParticipantRole.GUEST)
		);

		assertThrows(
				ForbiddenCompanionReportException.class,
				() -> setup.service.create(command(10L, 11L, List.of(ReportReason.BAD_ATTITUDE), "상세 내용"))
		);
	}

	@Test
	void allowsReportWithoutAnyCheckIn() {
		TestSetup setup = new TestSetup();
		CreateCompanionReportResult result = setup.service.create(command(10L, 11L, List.of(ReportReason.BAD_ATTITUDE), "상세 내용"));
		assertEquals(10L, result.matchId());
		assertEquals(1L, result.meetingId());
		assertEquals(1, setup.reportRepository.savedReports.size());
	}

	@Test
	void allowsReportAfterMeetingAndMatchCompleted() {
		TestSetup setup = new TestSetup();
		setup.meetingRepository.meeting = new CompanionMeeting(1L, 10L, CompanionMeetingStatus.COMPLETED, NOW, NOW);
		setup.matchRepository.status = CompanionMatchStatus.COMPLETED;
		setup.service.create(command(10L, 11L, List.of(ReportReason.BAD_ATTITUDE), "상세 내용"));
		assertEquals(1, setup.reportRepository.savedReports.size());
	}

	@Test
	void allowsReportBeforeScheduleConfirmation() {
		TestSetup setup = new TestSetup();
		setup.meetingRepository.meeting = null;
		setup.matchRepository.status = CompanionMatchStatus.MATCHED;
		CreateCompanionReportResult result = setup.service.create(matchCommand(11L));
		assertEquals(10L, result.matchId());
		assertNull(result.meetingId());
		assertNull(setup.reportRepository.savedReports.getFirst().meetingId());
		assertEquals(1, setup.mailSender.sent.size());
	}

	@Test
	void rejectsCanceledMatchBeforeScheduleConfirmation() {
		TestSetup setup = new TestSetup();
		setup.meetingRepository.meeting = null;
		setup.matchRepository.status = CompanionMatchStatus.CANCELED;
		assertThrows(CompanionReportAlreadyCanceledException.class, () -> setup.service.create(matchCommand(11L)));
		assertTrue(setup.reportRepository.savedReports.isEmpty());
	}

	@Test
	void rejectsCanceledMeetingThroughEitherApi() {
		TestSetup setup = new TestSetup();
		setup.meetingRepository.meeting = new CompanionMeeting(1L, 10L, CompanionMeetingStatus.CANCELED, NOW, null);
		assertThrows(CompanionReportAlreadyCanceledException.class, () -> setup.service.create(matchCommand(11L)));
		assertThrows(CompanionReportAlreadyCanceledException.class,
				() -> setup.service.create(command(10L, 11L, List.of(ReportReason.NO_SHOW), null)));
	}

	@Test
	void rejectsDuplicateAfterScheduleConfirmationAcrossApis() {
		TestSetup setup = new TestSetup();
		setup.meetingRepository.meeting = null;
		setup.matchRepository.status = CompanionMatchStatus.MATCHED;
		setup.service.create(matchCommand(11L));
		setup.meetingRepository.meeting = new CompanionMeeting(1L, 10L, CompanionMeetingStatus.ONGOING, NOW, null);
		setup.matchRepository.status = CompanionMatchStatus.SCHEDULE_CONFIRMED;
		assertThrows(CompanionReportAlreadyExistsException.class,
				() -> setup.service.create(command(10L, 11L, List.of(ReportReason.BAD_ATTITUDE), "상세 내용")));
		assertEquals(1, setup.reportRepository.savedReports.size());
		assertEquals(1, setup.mailSender.sent.size());
	}

	@Test
	void rejectsDuplicateFromMeetingApiToMatchApiButAllowsDifferentTarget() {
		TestSetup setup = new TestSetup();
		setup.service.create(command(10L, 11L, List.of(ReportReason.BAD_ATTITUDE), "상세 내용"));
		assertThrows(CompanionReportAlreadyExistsException.class, () -> setup.service.create(matchCommand(11L)));
		setup.service.create(matchCommand(12L));
		assertEquals(2, setup.reportRepository.savedReports.size());
	}

	@Test
	void rejectsSelfAndNonParticipantTarget() {
		TestSetup setup = new TestSetup();
		assertThrows(CompanionReportSelfNotAllowedException.class, () -> setup.service.create(matchCommand(10L)));
		assertThrows(CompanionReportTargetNotFoundException.class, () -> setup.service.create(matchCommand(99L)));
	}

	@Test
	void rejectsUnknownMatchAndMeeting() {
		TestSetup setup = new TestSetup();
		assertThrows(CompanionMatchNotFoundException.class, () -> setup.service.create(
				CreateCompanionReportCommand.forMatch(10L, 99L, 11L, List.of(ReportReason.NO_SHOW), null)));
		assertThrows(CompanionMeetingNotFoundException.class, () -> setup.service.create(
				new CreateCompanionReportCommand(10L, 99L, 11L, List.of(ReportReason.NO_SHOW), null)));
	}

	@Test
	void rejectsAmbiguousMissingAndNonpositiveIdentifiers() {
		TestSetup setup = new TestSetup();
		assertThrows(InvalidCompanionReportRequestException.class, () -> setup.service.create(null));
		for (CreateCompanionReportCommand command : List.of(
				new CreateCompanionReportCommand(10L, null, null, 11L, List.of(ReportReason.NO_SHOW), null),
				new CreateCompanionReportCommand(10L, 1L, 10L, 11L, List.of(ReportReason.NO_SHOW), null),
				CreateCompanionReportCommand.forMatch(10L, 0L, 11L, List.of(ReportReason.NO_SHOW), null),
				new CreateCompanionReportCommand(10L, -1L, 11L, List.of(ReportReason.NO_SHOW), null))) {
			assertThrows(InvalidCompanionReportRequestException.class, () -> setup.service.create(command));
		}
	}

	@Test
	void rejectsMissingAndDuplicateReasons() {
		TestSetup setup = new TestSetup();
		assertThrows(InvalidCompanionReportRequestException.class,
				() -> setup.service.create(command(10L, 11L, List.of(), null)));
		assertThrows(InvalidCompanionReportRequestException.class,
				() -> setup.service.create(command(10L, 11L, List.of(ReportReason.NO_SHOW, ReportReason.NO_SHOW), null)));
	}

	private CreateCompanionReportCommand matchCommand(final Long reportedUserId) {
		return CreateCompanionReportCommand.forMatch(10L, 10L, reportedUserId, List.of(ReportReason.NO_SHOW), null);
	}

	@Test
	void requiresDetailForEtcReason() {
		TestSetup setup = new TestSetup();

		assertThrows(
				InvalidCompanionReportRequestException.class,
				() -> setup.service.create(command(10L, 11L, List.of(ReportReason.ETC), " "))
		);
	}

	@Test
	void allowsReportWhenReportedUserHasNotCheckedIn() {
		TestSetup setup = new TestSetup();

		setup.service.create(command(10L, 11L, List.of(ReportReason.NO_SHOW), null));

		assertEquals(1, setup.reportRepository.savedReports.size());
	}

	@Test
	void resolvesReportedUserByNicknameWhenHostOmitsUserId() {
		TestSetup setup = new TestSetup();

		setup.service.create(command(10L, null, List.of(ReportReason.NO_SHOW), " 피신고자 "));

		assertEquals(11L, setup.reportRepository.savedReports.getFirst().reportedUserId());
	}

	@Test
	void resolvesReportedUserByNicknameWithUnicodeWhitespace() {
		TestSetup setup = new TestSetup();

		setup.service.create(command(10L, null, List.of(ReportReason.NO_SHOW), "\u3000피신고자\u3000"));

		assertEquals(11L, setup.reportRepository.savedReports.getFirst().reportedUserId());
	}

	@Test
	void rejectsMissingReportedUserIdFromGuest() {
		TestSetup setup = new TestSetup();

		assertThrows(
				InvalidCompanionReportRequestException.class,
				() -> setup.service.create(command(11L, null, List.of(ReportReason.BAD_ATTITUDE), "신고자"))
		);
	}

	@Test
	void rejectsUnknownNicknameWhenHostOmitsUserId() {
		TestSetup setup = new TestSetup();

		assertThrows(
				CompanionReportTargetNotFoundException.class,
				() -> setup.service.create(command(10L, null, List.of(ReportReason.NO_SHOW), "없는 닉네임"))
		);
	}

	@Test
	void requiresNicknameWhenHostOmitsUserId() {
		TestSetup setup = new TestSetup();

		assertThrows(
				InvalidCompanionReportRequestException.class,
				() -> setup.service.create(command(10L, null, List.of(ReportReason.NO_SHOW), " "))
		);
	}

	private CreateCompanionReportCommand command(
			final Long reporterUserId,
			final Long reportedUserId,
			final List<ReportReason> reasons,
			final String detail
	) {
		return new CreateCompanionReportCommand(reporterUserId, 1L, reportedUserId, reasons, detail);
	}

	private static final class TestSetup {
		private final MeetingRepository meetingRepository = new MeetingRepository();
		private final ParticipantRepository participantRepository = new ParticipantRepository();
		private final ProfileRepository profileRepository = new ProfileRepository();
		private final MatchRepository matchRepository = new MatchRepository();
		private final ReportRepository reportRepository = new ReportRepository();
		private final CapturingMailSender mailSender = new CapturingMailSender();
		private final CreateCompanionReportService service = new CreateCompanionReportService(
				meetingRepository,
				matchRepository,
				participantRepository,
				profileRepository,
				reportRepository,
				new ReportReasonRepository(),
				new MailContextQueryPort(),
				mailSender,
				CLOCK
		);
	}

	private static final class ProfileRepository implements CompanionProfileRepository {
		private final List<CompanionProfile> profiles = List.of(
				profile(1L, 10L, "신고자"),
				profile(2L, 11L, "피신고자"),
				profile(3L, 12L, "다른 참여자")
		);

		@Override
		public CompanionProfile save(final CompanionProfile model) {
			return model;
		}

		@Override
		public Optional<CompanionProfile> findById(final Long id) {
			return profiles.stream().filter(profile -> profile.id().equals(id)).findFirst();
		}

		@Override
		public List<CompanionProfile> findAllByUserIdIn(final List<Long> userIds) {
			return profiles.stream().filter(profile -> userIds.contains(profile.userId())).toList();
		}

		@Override
		public boolean existsByNickname(final String nickname) {
			return profiles.stream().anyMatch(profile -> profile.nickname().equals(nickname));
		}

		@Override
		public boolean existsByUserId(final Long userId) {
			return profiles.stream().anyMatch(profile -> profile.userId().equals(userId));
		}

		@Override
		public Optional<CompanionProfile> findByUserId(final Long userId) {
			return profiles.stream().filter(profile -> profile.userId().equals(userId)).findFirst();
		}

		@Override
		public Optional<CompanionProfile> findByUserIdForUpdate(final Long userId) {
			return findByUserId(userId);
		}

		private static CompanionProfile profile(final Long id, final Long userId, final String nickname) {
			return new CompanionProfile(
					id, userId, nickname, UserGender.MALE, 2000, null, null,
					BigDecimal.ZERO, 0, CompanionProfileStatus.ACTIVE
			);
		}
	}

	private static final class MeetingRepository implements CompanionMeetingRepository {
		private CompanionMeeting meeting = new CompanionMeeting(1L, 10L, CompanionMeetingStatus.ONGOING, NOW, null);

		@Override
		public CompanionMeeting save(final CompanionMeeting model) {
			return model;
		}

		@Override
		public Optional<CompanionMeeting> findById(final Long id) {
			return Optional.ofNullable(meeting).filter(value -> value.id().equals(id));
		}

		@Override
		public Optional<CompanionMeeting> findByMatchId(final Long matchId) {
			return Optional.ofNullable(meeting).filter(value -> value.matchId().equals(matchId));
		}

		@Override
		public boolean completeIfOngoing(final Long meetingId, final LocalDateTime completedAt) {
			return false;
		}
	}

	private static final class ParticipantRepository implements CompanionMatchParticipantRepository {
		private List<CompanionMatchParticipant> participants = List.of(
				new CompanionMatchParticipant(1L, 10L, 10L, null, MatchParticipantRole.HOST),
				new CompanionMatchParticipant(2L, 10L, 11L, null, MatchParticipantRole.GUEST),
				new CompanionMatchParticipant(3L, 10L, 12L, null, MatchParticipantRole.GUEST)
		);
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
			return participants;
		}

		@Override
		public boolean existsByMatchIdAndUserId(final Long matchId, final Long userId) {
			return true;
		}
	}

	private static final class MatchRepository implements CompanionMatchRepository {
		private CompanionMatchStatus status = CompanionMatchStatus.SCHEDULE_CONFIRMED;
		private int findByIdForUpdateCalls;

		@Override
		public CompanionMatch save(final CompanionMatch model) { return model; }

		@Override
		public Optional<CompanionMatch> findById(final Long id) {
			return id.equals(10L) ? Optional.of(new CompanionMatch(10L, 20L, status, NOW)) : Optional.empty();
		}

		@Override
		public Optional<CompanionMatch> findByIdForUpdate(final Long id) {
			findByIdForUpdateCalls++;
			return findById(id);
		}

		@Override
		public Optional<CompanionMatch> findFirstByPostIdAndStatus(final Long postId, final CompanionMatchStatus status) {
			return Optional.empty();
		}

		@Override
		public boolean confirmScheduleIfMatched(final Long matchId) { return false; }
	}

	private static final class ReportRepository implements CompanionReportRepository {
		private final List<CompanionReport> savedReports = new ArrayList<>();

		@Override
		public CompanionReport save(final CompanionReport model) {
			CompanionReport saved = new CompanionReport(1L, model.matchId(), model.meetingId(), model.reporterUserId(), model.reportedUserId(),
					model.detail(), model.createdAt());
			savedReports.add(saved);
			return saved;
		}

		@Override
		public Optional<CompanionReport> findById(final Long id) {
			return Optional.empty();
		}

		@Override
		public boolean existsByMatchIdAndReporterUserIdAndReportedUserId(
				final Long matchId,
				final Long reporterUserId,
				final Long reportedUserId
		) {
			return savedReports.stream().anyMatch(report -> report.matchId().equals(matchId)
					&& report.reporterUserId().equals(reporterUserId) && report.reportedUserId().equals(reportedUserId));
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
		public Optional<CompanionReportMailContext> findByMatchIdAndUserIds(
				final Long matchId,
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
