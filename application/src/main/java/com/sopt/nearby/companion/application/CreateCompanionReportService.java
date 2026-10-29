// 동행 신고 검증과 저장 및 신고 이메일 발송을 처리하는 유스케이스 구현체
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReportAlreadyExistsException;
import com.sopt.nearby.companion.domain.exception.CompanionReportContextNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReportAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionReportSelfNotAllowedException;
import com.sopt.nearby.companion.domain.exception.CompanionReportTargetNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionReportException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionReportRequestException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.report.CompanionReport;
import com.sopt.nearby.companion.domain.model.report.CompanionReportReason;
import com.sopt.nearby.companion.domain.model.report.ReportReason;
import com.sopt.nearby.companion.port.in.CreateCompanionReportUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.companion.port.out.CompanionReportMail;
import com.sopt.nearby.companion.port.out.CompanionReportMailContext;
import com.sopt.nearby.companion.port.out.CompanionReportMailContextQueryPort;
import com.sopt.nearby.companion.port.out.CompanionReportMailSender;
import com.sopt.nearby.companion.port.out.CompanionReportReasonRepository;
import com.sopt.nearby.companion.port.out.CompanionReportRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class CreateCompanionReportService implements CreateCompanionReportUseCase {

	private final CompanionMeetingRepository meetingRepository;
	private final CompanionMatchRepository matchRepository;
	private final CompanionMatchParticipantRepository participantRepository;
	private final CompanionProfileRepository profileRepository;
	private final CompanionReportRepository reportRepository;
	private final CompanionReportReasonRepository reportReasonRepository;
	private final CompanionReportMailContextQueryPort mailContextQueryPort;
	private final CompanionReportMailSender mailSender;
	private final Clock clock;

	public CreateCompanionReportService(
			final CompanionMeetingRepository meetingRepository,
			final CompanionMatchRepository matchRepository,
			final CompanionMatchParticipantRepository participantRepository,
			final CompanionProfileRepository profileRepository,
			final CompanionReportRepository reportRepository,
			final CompanionReportReasonRepository reportReasonRepository,
			final CompanionReportMailContextQueryPort mailContextQueryPort,
			final CompanionReportMailSender mailSender,
			final Clock clock
	) {
		this.meetingRepository = meetingRepository;
		this.matchRepository = matchRepository;
		this.participantRepository = participantRepository;
		this.profileRepository = profileRepository;
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

		CompanionMeeting meeting = command.meetingId() == null ? null
				: meetingRepository.findById(command.meetingId()).orElseThrow(CompanionMeetingNotFoundException::new);
		Long matchId = meeting == null ? command.matchId() : meeting.matchId();
		CompanionMatch match = matchRepository.findByIdForUpdate(matchId)
				.orElseThrow(CompanionMatchNotFoundException::new);
		if (meeting == null) {
			meeting = meetingRepository.findByMatchId(matchId).orElse(null);
		}

		List<CompanionMatchParticipant> participants = participantRepository.findAllByMatchId(matchId);
		Long reportedUserId = resolveReportedUserId(command, participants);
		validateParticipants(participants, command.reporterUserId(), reportedUserId);
		validateStatus(match, meeting);
		validateReasons(command.reasons(), command.detail());
		if (reportRepository.existsByMatchIdAndReporterUserIdAndReportedUserId(
				matchId, command.reporterUserId(), reportedUserId)) {
			throw new CompanionReportAlreadyExistsException();
		}

		CompanionReportMailContext mailContext = mailContextQueryPort
				.findByMatchIdAndUserIds(matchId, command.reporterUserId(), reportedUserId)
				.orElseThrow(CompanionReportContextNotFoundException::new);

		LocalDateTime now = LocalDateTime.now(clock);
		Long meetingId = meeting == null ? null : meeting.id();
		CompanionReport savedReport = reportRepository.save(new CompanionReport(
				null,
				matchId,
				meetingId,
				command.reporterUserId(),
				reportedUserId,
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

		var zone = CompanionPlaceCityNameResolver.resolveSupportedCity(mailContext.placeAddress())
				.map(city -> city.zoneId()).orElse(ZoneOffset.UTC);
		return new CreateCompanionReportResult(meetingId, matchId, savedReport.id(),
				CompanionMeetingTimeConverter.utcToLocal(savedReport.createdAt(), zone));
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
				|| (command.meetingId() == null) == (command.matchId() == null)
				|| (command.meetingId() != null && command.meetingId() <= 0)
				|| (command.matchId() != null && command.matchId() <= 0)
				|| (command.reportedUserId() != null && command.reportedUserId() <= 0)) {
			throw new InvalidCompanionReportRequestException();
		}
	}

	private Long resolveReportedUserId(
			final CreateCompanionReportCommand command,
			final List<CompanionMatchParticipant> participants
	) {
		if (command.reportedUserId() != null) {
			return command.reportedUserId();
		}
		CompanionMatchParticipant reporter = participants.stream()
				.filter(participant -> participant.userId().equals(command.reporterUserId()))
				.findFirst()
				.orElseThrow(ForbiddenCompanionReportException::new);
		if (reporter.role() != MatchParticipantRole.HOST
				|| command.detail() == null || command.detail().isBlank()) {
			throw new InvalidCompanionReportRequestException();
		}
		List<Long> guestUserIds = participants.stream()
				.filter(participant -> participant.role() == MatchParticipantRole.GUEST)
				.map(CompanionMatchParticipant::userId)
				.toList();
		return profileRepository.findAllByUserIdIn(guestUserIds).stream()
				.filter(profile -> profile.nickname().equals(command.detail().strip()))
				.map(CompanionProfile::userId)
				.findFirst()
				.orElseThrow(CompanionReportTargetNotFoundException::new);
	}

	private void validateStatus(final CompanionMatch match, final CompanionMeeting meeting) {
		if (match.status() == CompanionMatchStatus.CANCELED
				|| (meeting != null && meeting.status() == CompanionMeetingStatus.CANCELED)) {
			throw new CompanionReportAlreadyCanceledException();
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
