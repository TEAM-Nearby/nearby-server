// 동행 신고 검증과 저장 및 신고 이메일 발송을 처리하는 유스케이스 구현체
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReportAlreadyExistsException;
import com.sopt.nearby.companion.domain.exception.CompanionReportContextNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReportCurrentUserAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompanionReportCurrentUserNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.CompanionReportMeetingNotOngoingException;
import com.sopt.nearby.companion.domain.exception.CompanionReportSelfNotAllowedException;
import com.sopt.nearby.companion.domain.exception.CompanionReportTargetNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionReportException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionReportRequestException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import com.sopt.nearby.companion.domain.model.report.CompanionReport;
import com.sopt.nearby.companion.domain.model.report.CompanionReportReason;
import com.sopt.nearby.companion.domain.model.report.ReportReason;
import com.sopt.nearby.companion.port.in.CreateCompanionReportUseCase;
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
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class CreateCompanionReportService implements CreateCompanionReportUseCase {

	private final CompanionMeetingRepository meetingRepository;
	private final CompanionMatchParticipantRepository participantRepository;
	private final MeetingCheckInRepository checkInRepository;
	private final CompanionReportRepository reportRepository;
	private final CompanionReportReasonRepository reportReasonRepository;
	private final CompanionReportMailContextQueryPort mailContextQueryPort;
	private final CompanionReportMailSender mailSender;
	private final Clock clock;

	public CreateCompanionReportService(
			final CompanionMeetingRepository meetingRepository,
			final CompanionMatchParticipantRepository participantRepository,
			final MeetingCheckInRepository checkInRepository,
			final CompanionReportRepository reportRepository,
			final CompanionReportReasonRepository reportReasonRepository,
			final CompanionReportMailContextQueryPort mailContextQueryPort,
			final CompanionReportMailSender mailSender,
			final Clock clock
	) {
		this.meetingRepository = meetingRepository;
		this.participantRepository = participantRepository;
		this.checkInRepository = checkInRepository;
		this.reportRepository = reportRepository;
		this.reportReasonRepository = reportReasonRepository;
		this.mailContextQueryPort = mailContextQueryPort;
		this.mailSender = mailSender;
		this.clock = clock;
	}

	@Override
	@Transactional
	public CreateCompanionReportResult create(final CreateCompanionReportCommand command) {
		validateCommand(command);

		CompanionMeeting meeting = meetingRepository.findByIdForUpdate(command.meetingId())
				.orElseThrow(CompanionMeetingNotFoundException::new);
		validateMeeting(meeting);

		List<CompanionMatchParticipant> participants = participantRepository.findAllByMatchId(meeting.matchId());
		validateParticipants(participants, command.reporterUserId(), command.reportedUserId());
		MeetingCheckIn reporterCheckIn = checkInRepository
				.findByMeetingIdAndUserId(meeting.id(), command.reporterUserId())
				.orElseThrow(CompanionReportCurrentUserNotCheckedInException::new);
		if (reporterCheckIn.completedAt() != null) {
			throw new CompanionReportCurrentUserAlreadyCompletedException();
		}
		validateReasons(command.reasons(), command.detail());
		if (reportRepository.existsByMeetingIdAndReporterUserIdAndReportedUserId(
				meeting.id(), command.reporterUserId(), command.reportedUserId())) {
			throw new CompanionReportAlreadyExistsException();
		}

		CompanionReportMailContext mailContext = mailContextQueryPort
				.findByMeetingIdAndUserIds(meeting.id(), command.reporterUserId(), command.reportedUserId())
				.orElseThrow(CompanionReportContextNotFoundException::new);

		LocalDateTime now = LocalDateTime.now(clock);
		CompanionReport savedReport = reportRepository.save(new CompanionReport(
				null,
				meeting.id(),
				command.reporterUserId(),
				command.reportedUserId(),
				command.detail(),
				now
		));
		command.reasons().forEach(reason -> reportReasonRepository.save(
				new CompanionReportReason(savedReport.id(), reason)
		));

		CompanionReportMail mail = new CompanionReportMail(
				savedReport,
				List.copyOf(command.reasons()),
				mailContext.reporterNickname(),
				mailContext.reporterPhoneNumber(),
				mailContext.reportedNickname(),
				mailContext.reportedPhoneNumber(),
				mailContext.companionPostId(),
				mailContext.companionPostContent(),
				mailContext.scheduledAt(),
				mailContext.meetingTimeType(),
				mailContext.placeAddress()
		);
		sendMailAfterCommit(mail);

		return new CreateCompanionReportResult(meeting.id(), savedReport.id(), savedReport.createdAt());
	}

	private void sendMailAfterCommit(final CompanionReportMail mail) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			mailSender.send(mail);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				mailSender.send(mail);
			}
		});
	}

	private void validateCommand(final CreateCompanionReportCommand command) {
		if (command == null || command.reporterUserId() == null || command.reporterUserId() <= 0
				|| command.meetingId() == null || command.meetingId() <= 0
				|| command.reportedUserId() == null || command.reportedUserId() <= 0) {
			throw new InvalidCompanionReportRequestException();
		}
	}

	private void validateMeeting(final CompanionMeeting meeting) {
		if (meeting.status() != CompanionMeetingStatus.ONGOING) {
			throw new CompanionReportMeetingNotOngoingException();
		}
	}

	private void validateParticipants(
			final List<CompanionMatchParticipant> participants,
			final Long reporterUserId,
			final Long reportedUserId
	) {
		if (reporterUserId.equals(reportedUserId)) {
			throw new CompanionReportSelfNotAllowedException();
		}
		if (participants.stream().noneMatch(participant -> participant.userId().equals(reporterUserId))) {
			throw new ForbiddenCompanionReportException();
		}
		if (participants.stream().noneMatch(participant -> participant.userId().equals(reportedUserId))) {
			throw new CompanionReportTargetNotFoundException();
		}
	}

	private void validateReasons(final List<ReportReason> reasons, final String detail) {
		if (reasons == null || reasons.isEmpty() || reasons.stream().anyMatch(reason -> reason == null)) {
			throw new InvalidCompanionReportRequestException();
		}
		List<ReportReason> distinctReasons = List.copyOf(new LinkedHashSet<>(reasons));
		if (distinctReasons.size() != reasons.size()) {
			throw new InvalidCompanionReportRequestException();
		}
		if (reasons.contains(ReportReason.ETC) && (detail == null || detail.isBlank())) {
			throw new InvalidCompanionReportRequestException();
		}
	}
}
