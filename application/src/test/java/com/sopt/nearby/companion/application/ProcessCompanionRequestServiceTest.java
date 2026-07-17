// 동행 신청 수락과 거절 서비스의 상태 변경과 매칭 생성을 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionPostCapacityReachedException;
import com.sopt.nearby.companion.domain.exception.CompanionPostNotRecruitingException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestNotPendingException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionRequestHostOnlyException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionRequestSelfException;
import com.sopt.nearby.companion.domain.model.match.CompanionApplication;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionSchedule;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.port.in.CreateCompanionNotificationUseCase;
import com.sopt.nearby.companion.port.out.CompanionApplicationRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessCompanionRequestServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-15T03:30:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 12, 30);

    private FakeCompanionApplicationRepository applicationRepository;
    private FakeCompanionPostRepository postRepository;
    private FakeCompanionMatchRepository matchRepository;
    private FakeCompanionMatchParticipantRepository participantRepository;
    private FakeCompanionScheduleRepository scheduleRepository;
    private FakeCompanionMeetingRepository meetingRepository;
    private FakeCreateCompanionNotificationUseCase notificationUseCase;
    private ProcessCompanionRequestService service;

    @BeforeEach
    void setUp() {
        applicationRepository = new FakeCompanionApplicationRepository();
        postRepository = new FakeCompanionPostRepository();
        matchRepository = new FakeCompanionMatchRepository();
        participantRepository = new FakeCompanionMatchParticipantRepository();
        scheduleRepository = new FakeCompanionScheduleRepository();
        meetingRepository = new FakeCompanionMeetingRepository();
        notificationUseCase = new FakeCreateCompanionNotificationUseCase();
        service = new ProcessCompanionRequestService(
                applicationRepository,
                postRepository,
                matchRepository,
                participantRepository,
                scheduleRepository,
                meetingRepository,
                notificationUseCase,
                CLOCK
        );
    }

    @Test
    void acceptsPendingRequestAndCreatesMatchGroupWhenPostHasNoMatchedMatch() {
        applicationRepository.save(application(3L, 10L, 7L, CompanionApplicationStatus.PENDING, null));
        postRepository.save(post(10L, 100L));

        AcceptedCompanionRequestResult result = service.accept(new AcceptCompanionRequestCommand(100L, 3L));

        assertEquals(3L, result.applicationId());
        assertEquals(10L, result.postId());
        assertEquals(CompanionApplicationStatus.ACCEPTED, result.applicationStatus());
        assertEquals(1L, result.matchId());
        assertEquals(CompanionMatchStatus.MATCHED, result.matchStatus());
        assertEquals(List.of(10L), postRepository.lockedPostIds);
        assertEquals(CompanionApplicationStatus.ACCEPTED, applicationRepository.findById(3L).get().status());
        assertEquals(1, matchRepository.matches.size());
        assertTrue(participantRepository.existsByMatchIdAndUserId(1L, 100L));
        assertTrue(participantRepository.existsByMatchIdAndUserId(1L, 7L));
        assertEquals(MatchParticipantRole.HOST, participantRepository.findByMatchIdAndUserId(1L, 100L).role());
        CompanionMatchParticipant guest = participantRepository.findByMatchIdAndUserId(1L, 7L);
        assertEquals(MatchParticipantRole.GUEST, guest.role());
        assertEquals(3L, guest.acceptedApplicationId());
        assertEquals(CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                notificationUseCase.commands.get(0).notificationType());
        assertEquals(7L, notificationUseCase.commands.get(0).recipientUserId());
    }

    @Test
    void acceptsPendingRequestAndReusesExistingMatchedMatchGroup() {
        applicationRepository.save(application(3L, 10L, 7L, CompanionApplicationStatus.PENDING, null));
        postRepository.save(post(10L, 100L));
        matchRepository.save(new CompanionMatch(50L, 10L, CompanionMatchStatus.MATCHED, NOW.minusMinutes(1)));
        participantRepository.save(new CompanionMatchParticipant(null, 50L, 100L, null, MatchParticipantRole.HOST));

        AcceptedCompanionRequestResult result = service.accept(new AcceptCompanionRequestCommand(100L, 3L));

        assertEquals(50L, result.matchId());
        assertEquals(1, matchRepository.matches.size());
        assertTrue(participantRepository.existsByMatchIdAndUserId(50L, 100L));
        assertTrue(participantRepository.existsByMatchIdAndUserId(50L, 7L));
        assertEquals(3L, participantRepository.findByMatchIdAndUserId(50L, 7L).acceptedApplicationId());
    }

    @Test
    void acceptsNowRequestAndConfirmsMatchScheduleAutomatically() {
        LocalDateTime applicationCreatedAt = NOW.minusMinutes(20);
        applicationRepository.save(application(
                3L,
                10L,
                7L,
                CompanionApplicationStatus.PENDING,
                null,
                applicationCreatedAt
        ));
        postRepository.save(nowPost(10L, 100L));

        AcceptedCompanionRequestResult result = service.accept(new AcceptCompanionRequestCommand(100L, 3L));

        assertEquals(CompanionMatchStatus.SCHEDULE_CONFIRMED, result.matchStatus());
        assertEquals(applicationCreatedAt, result.meetingAt());
        CompanionMatch match = matchRepository.findById(result.matchId()).orElseThrow();
        assertEquals(CompanionMatchStatus.SCHEDULE_CONFIRMED, match.status());
        CompanionSchedule schedule = scheduleRepository.findConfirmedByMatchId(result.matchId()).orElseThrow();
        assertEquals(20L, schedule.placeId());
        assertEquals(applicationCreatedAt, schedule.scheduledAt());
        assertTrue(schedule.confirmed());
        CompanionMeeting meeting = meetingRepository.meetings.values().stream().findFirst().orElseThrow();
        assertEquals(result.matchId(), meeting.matchId());
        assertEquals(CompanionMeetingStatus.ONGOING, meeting.status());
        assertEquals(applicationCreatedAt, meeting.startedAt());
    }

    @Test
    void rejectsExpiredNowRequestBeforeAcceptingApplication() {
        applicationRepository.save(application(3L, 10L, 7L, CompanionApplicationStatus.PENDING, null));
        postRepository.save(nowPost(10L, 100L, NOW));

        assertThrows(
                CompanionPostNotRecruitingException.class,
                () -> service.accept(new AcceptCompanionRequestCommand(100L, 3L))
        );

        assertEquals(CompanionApplicationStatus.PENDING, applicationRepository.findById(3L).orElseThrow().status());
        assertTrue(matchRepository.matches.isEmpty());
        assertTrue(participantRepository.participants.isEmpty());
        assertTrue(scheduleRepository.schedules.isEmpty());
        assertTrue(meetingRepository.meetings.isEmpty());
        assertTrue(notificationUseCase.commands.isEmpty());
    }

    @Test
    void rejectsAcceptanceWhenPostCapacityIsReached() {
        applicationRepository.save(application(1L, 10L, 7L, CompanionApplicationStatus.ACCEPTED, null));
        applicationRepository.save(application(2L, 10L, 8L, CompanionApplicationStatus.PENDING, null));
        postRepository.save(post(10L, 100L, 2));

        assertThrows(
                CompanionPostCapacityReachedException.class,
                () -> service.accept(new AcceptCompanionRequestCommand(100L, 2L))
        );

        assertEquals(CompanionApplicationStatus.PENDING, applicationRepository.findById(2L).orElseThrow().status());
        assertTrue(matchRepository.matches.isEmpty());
        assertTrue(notificationUseCase.commands.isEmpty());
    }

    @Test
    void acceptsNowRequestAndReusesExistingScheduleConfirmedMatchGroup() {
        applicationRepository.save(application(3L, 10L, 7L, CompanionApplicationStatus.PENDING, null));
        postRepository.save(nowPost(10L, 100L));
        matchRepository.save(
                new CompanionMatch(50L, 10L, CompanionMatchStatus.SCHEDULE_CONFIRMED, NOW.minusMinutes(1)));
        LocalDateTime existingScheduleAt = NOW.minusMinutes(30);
        scheduleRepository.save(new CompanionSchedule(null, 50L, 20L, existingScheduleAt, null, true));
        meetingRepository.save(new CompanionMeeting(
                null,
                50L,
                CompanionMeetingStatus.ONGOING,
                existingScheduleAt,
                null
        ));
        participantRepository.save(new CompanionMatchParticipant(null, 50L, 100L, null, MatchParticipantRole.HOST));

        AcceptedCompanionRequestResult result = service.accept(new AcceptCompanionRequestCommand(100L, 3L));

        assertEquals(50L, result.matchId());
        assertEquals(CompanionMatchStatus.SCHEDULE_CONFIRMED, result.matchStatus());
        assertEquals(existingScheduleAt, result.meetingAt());
        assertEquals(1, matchRepository.matches.size());
        assertEquals(1, scheduleRepository.schedules.size());
        assertEquals(1, meetingRepository.meetings.size());
        assertEquals(existingScheduleAt, scheduleRepository.findConfirmedByMatchId(50L).orElseThrow().scheduledAt());
        assertTrue(participantRepository.existsByMatchIdAndUserId(50L, 7L));
    }

    @Test
    void rejectsNowRequestWhenMatchWasCanceledBeforeAutoConfirm() {
        applicationRepository.save(application(3L, 10L, 7L, CompanionApplicationStatus.PENDING, null));
        postRepository.save(nowPost(10L, 100L));
        matchRepository.save(new CompanionMatch(50L, 10L, CompanionMatchStatus.MATCHED, NOW.minusMinutes(1)));
        matchRepository.confirmScheduleResult = false;
        matchRepository.latestAfterFailedConfirm = new CompanionMatch(
                50L,
                10L,
                CompanionMatchStatus.CANCELED,
                NOW.minusMinutes(1)
        );

        assertThrows(
                CompanionMatchAlreadyCanceledException.class,
                () -> service.accept(new AcceptCompanionRequestCommand(100L, 3L))
        );

        assertEquals(1, matchRepository.confirmScheduleAttempts);
        assertTrue(scheduleRepository.schedules.isEmpty());
        assertTrue(meetingRepository.meetings.isEmpty());
    }

    @Test
    void rejectsPendingRequestAndStoresReasonOnlyInApplication() {
        applicationRepository.save(application(3L, 10L, 7L, CompanionApplicationStatus.PENDING, null));
        postRepository.save(post(10L, 100L));

        RejectedCompanionRequestResult result = service.reject(
                new RejectCompanionRequestCommand(100L, 3L, "일정이 맞지 않아요")
        );

        assertEquals(3L, result.applicationId());
        assertEquals(10L, result.postId());
        assertEquals(CompanionApplicationStatus.REJECTED, result.applicationStatus());
        CompanionApplication rejected = applicationRepository.findById(3L).get();
        assertEquals(CompanionApplicationStatus.REJECTED, rejected.status());
        assertEquals("일정이 맞지 않아요", rejected.rejectionReason());
        assertEquals(0, matchRepository.matches.size());
        assertEquals(CompanionNotificationType.COMPANION_APPLICATION_REJECTED,
                notificationUseCase.commands.get(0).notificationType());
        assertEquals(7L, notificationUseCase.commands.get(0).recipientUserId());
    }

    @Test
    void rejectsMissingRequest() {
        assertThrows(
                CompanionRequestNotFoundException.class,
                () -> service.accept(new AcceptCompanionRequestCommand(100L, 3L))
        );
    }

    @Test
    void rejectsNonHostUser() {
        applicationRepository.save(application(3L, 10L, 7L, CompanionApplicationStatus.PENDING, null));
        postRepository.save(post(10L, 100L));

        assertThrows(
                ForbiddenCompanionRequestHostOnlyException.class,
                () -> service.reject(new RejectCompanionRequestCommand(99L, 3L, null))
        );
        assertEquals(CompanionApplicationStatus.PENDING, applicationRepository.findById(3L).get().status());
        assertTrue(notificationUseCase.commands.isEmpty());
    }

    @Test
    void rejectsHostSelfApplicationBeforeStatusChange() {
        applicationRepository.save(application(3L, 10L, 100L, CompanionApplicationStatus.PENDING, null));
        postRepository.save(post(10L, 100L));

        assertThrows(
                ForbiddenCompanionRequestSelfException.class,
                () -> service.accept(new AcceptCompanionRequestCommand(100L, 3L))
        );
        assertEquals(CompanionApplicationStatus.PENDING, applicationRepository.findById(3L).get().status());
        assertTrue(matchRepository.matches.isEmpty());
        assertTrue(participantRepository.participants.isEmpty());
        assertTrue(notificationUseCase.commands.isEmpty());
    }

    @Test
    void rejectsAlreadyProcessedRequest() {
        applicationRepository.save(application(3L, 10L, 7L, CompanionApplicationStatus.ACCEPTED, null));
        postRepository.save(post(10L, 100L));

        assertThrows(
                CompanionRequestNotPendingException.class,
                () -> service.accept(new AcceptCompanionRequestCommand(100L, 3L))
        );
    }

    @Test
    void rejectsConcurrentStatusChangeFromPending() {
        applicationRepository.save(application(3L, 10L, 7L, CompanionApplicationStatus.PENDING, null));
        postRepository.save(post(10L, 100L));
        applicationRepository.failNextPendingUpdate = true;

        assertThrows(
                CompanionRequestNotPendingException.class,
                () -> service.reject(new RejectCompanionRequestCommand(100L, 3L, null))
        );
        assertFalse(applicationRepository.findById(3L).get().status() == CompanionApplicationStatus.REJECTED);
        assertTrue(notificationUseCase.commands.isEmpty());
    }

    private CompanionApplication application(
            final Long id,
            final Long postId,
            final Long applicantUserId,
            final CompanionApplicationStatus status,
            final String rejectionReason
    ) {
        return application(id, postId, applicantUserId, status, rejectionReason, NOW.minusHours(1));
    }

    private CompanionApplication application(
            final Long id,
            final Long postId,
            final Long applicantUserId,
            final CompanionApplicationStatus status,
            final String rejectionReason,
            final LocalDateTime createdAt
    ) {
        return new CompanionApplication(id, postId, applicantUserId, status, rejectionReason, createdAt);
    }

    private CompanionPost post(final Long id, final Long hostUserId) {
        return post(id, hostUserId, 4);
    }

    private CompanionPost post(final Long id, final Long hostUserId, final int maxParticipants) {
        return new CompanionPost(
                id,
                hostUserId,
                20L,
                CompanionPostMeetingTimeType.SCHEDULED,
                NOW.plusHours(2),
                null,
                maxParticipants,
                true,
                "같이 밥 먹을 동행을 구해요.",
                "https://open.kakao.com/o/nearby123",
                CompanionPostStatus.RECRUITING,
                NOW.minusHours(1)
        );
    }

    private CompanionPost nowPost(final Long id, final Long hostUserId) {
        return nowPost(id, hostUserId, NOW.plusHours(1));
    }

    private CompanionPost nowPost(final Long id, final Long hostUserId, final LocalDateTime exposureExpiresAt) {
        return new CompanionPost(
                id,
                hostUserId,
                20L,
                CompanionPostMeetingTimeType.NOW,
                null,
                exposureExpiresAt,
                4,
                true,
                "지금 바로 같이 밥 먹을 동행을 구해요.",
                "https://open.kakao.com/o/nearby123",
                CompanionPostStatus.RECRUITING,
                NOW.minusHours(1)
        );
    }

    private static final class FakeCompanionApplicationRepository implements CompanionApplicationRepository {

        private final Map<Long, CompanionApplication> applications = new HashMap<>();
        private boolean failNextPendingUpdate;

        @Override
        public CompanionApplication save(final CompanionApplication model) {
            applications.put(model.id(), model);
            return model;
        }

        @Override
        public Optional<CompanionApplication> findById(final Long id) {
            return Optional.ofNullable(applications.get(id));
        }

        @Override
        public boolean existsByPostIdAndApplicantUserId(final Long postId, final Long applicantUserId) {
            return applications.values()
                    .stream()
                    .anyMatch(application -> application.postId().equals(postId)
                            && application.applicantUserId().equals(applicantUserId));
        }

        @Override
        public long countAcceptedByPostId(final Long postId) {
            return applications.values()
                    .stream()
                    .filter(application -> application.postId().equals(postId))
                    .filter(application -> application.status() == CompanionApplicationStatus.ACCEPTED)
                    .count();
        }

        @Override
        public boolean updateStatusIfPending(
                final Long applicationId,
                final CompanionApplicationStatus status,
                final String rejectionReason
        ) {
            if (failNextPendingUpdate) {
                return false;
            }
            CompanionApplication application = applications.get(applicationId);
            if (application == null || application.status() != CompanionApplicationStatus.PENDING) {
                return false;
            }
            applications.put(applicationId, new CompanionApplication(
                    application.id(),
                    application.postId(),
                    application.applicantUserId(),
                    status,
                    rejectionReason,
                    application.createdAt()
            ));
            return true;
        }
    }

    private static final class FakeCompanionPostRepository implements CompanionPostRepository {

        private final Map<Long, CompanionPost> posts = new HashMap<>();
        private final List<Long> lockedPostIds = new ArrayList<>();

        @Override
        public CompanionPost save(final CompanionPost model) {
            posts.put(model.id(), model);
            return model;
        }

        @Override
        public Optional<CompanionPost> findById(final Long id) {
            return Optional.ofNullable(posts.get(id));
        }

        public Optional<CompanionPost> findByIdForUpdate(final Long id) {
            lockedPostIds.add(id);
            return findById(id);
        }
    }

    private static final class FakeCompanionMatchRepository implements CompanionMatchRepository {

        private final Map<Long, CompanionMatch> matches = new HashMap<>();
        private long nextId = 1L;
        private boolean confirmScheduleResult = true;
        private CompanionMatch latestAfterFailedConfirm;
        private int confirmScheduleAttempts;

        @Override
        public CompanionMatch save(final CompanionMatch model) {
            CompanionMatch saved = model.id() == null
                    ? new CompanionMatch(nextId++, model.postId(), model.status(), model.createdAt())
                    : model;
            matches.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Optional<CompanionMatch> findById(final Long id) {
            return Optional.ofNullable(matches.get(id));
        }

        @Override
        public boolean confirmScheduleIfMatched(final Long matchId) {
            confirmScheduleAttempts++;
            CompanionMatch match = matches.get(matchId);
            if (match == null || match.status() != CompanionMatchStatus.MATCHED) {
                return false;
            }
            if (!confirmScheduleResult) {
                if (latestAfterFailedConfirm != null) {
                    matches.put(matchId, latestAfterFailedConfirm);
                }
                return false;
            }
            matches.put(matchId, new CompanionMatch(
                    match.id(),
                    match.postId(),
                    CompanionMatchStatus.SCHEDULE_CONFIRMED,
                    match.createdAt()
            ));
            return true;
        }

        @Override
        public Optional<CompanionMatch> findFirstByPostIdAndStatus(
                final Long postId,
                final CompanionMatchStatus status
        ) {
            return matches.values()
                    .stream()
                    .filter(match -> match.postId().equals(postId))
                    .filter(match -> match.status() == status)
                    .findFirst();
        }
    }

    private static final class FakeCompanionScheduleRepository implements CompanionScheduleRepository {

        private final Map<Long, CompanionSchedule> schedules = new HashMap<>();
        private long nextId = 1L;

        @Override
        public CompanionSchedule save(final CompanionSchedule model) {
            CompanionSchedule saved = model.id() == null
                    ? new CompanionSchedule(
                    nextId++,
                    model.matchId(),
                    model.placeId(),
                    model.scheduledAt(),
                    model.estimatedDurationMinutes(),
                    model.confirmed()
            )
                    : model;
            schedules.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Optional<CompanionSchedule> findById(final Long id) {
            return Optional.ofNullable(schedules.get(id));
        }

        @Override
        public Optional<CompanionSchedule> findConfirmedByMatchId(final Long matchId) {
            return schedules.values()
                    .stream()
                    .filter(schedule -> schedule.matchId().equals(matchId))
                    .filter(CompanionSchedule::confirmed)
                    .findFirst();
        }
    }

    private static final class FakeCompanionMatchParticipantRepository
            implements CompanionMatchParticipantRepository {

        private final List<CompanionMatchParticipant> participants = new ArrayList<>();
        private long nextId = 1L;

        @Override
        public CompanionMatchParticipant save(final CompanionMatchParticipant model) {
            CompanionMatchParticipant saved = new CompanionMatchParticipant(
                    nextId++,
                    model.matchId(),
                    model.userId(),
                    model.acceptedApplicationId(),
                    model.role()
            );
            participants.add(saved);
            return saved;
        }

        @Override
        public Optional<CompanionMatchParticipant> findById(final Long id) {
            return participants.stream()
                    .filter(participant -> participant.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<CompanionMatchParticipant> findAllByMatchId(final Long matchId) {
            return participants.stream()
                    .filter(participant -> participant.matchId().equals(matchId))
                    .toList();
        }

        @Override
        public boolean existsByMatchIdAndUserId(final Long matchId, final Long userId) {
            return participants.stream()
                    .anyMatch(participant -> participant.matchId().equals(matchId)
                            && participant.userId().equals(userId));
        }

        private CompanionMatchParticipant findByMatchIdAndUserId(final Long matchId, final Long userId) {
            return participants.stream()
                    .filter(participant -> participant.matchId().equals(matchId))
                    .filter(participant -> participant.userId().equals(userId))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private static final class FakeCompanionMeetingRepository implements CompanionMeetingRepository {

        private final Map<Long, CompanionMeeting> meetings = new HashMap<>();
        private long nextId = 1L;

        @Override
        public CompanionMeeting save(final CompanionMeeting model) {
            CompanionMeeting saved = model.id() == null
                    ? new CompanionMeeting(
                    nextId++,
                    model.matchId(),
                    model.status(),
                    model.startedAt(),
                    model.completedAt()
            )
                    : model;
            meetings.put(saved.id(), saved);
            return saved;
        }

        @Override
        public Optional<CompanionMeeting> findById(final Long id) {
            return Optional.ofNullable(meetings.get(id));
        }

        @Override
        public boolean completeIfOngoing(final Long meetingId, final LocalDateTime completedAt) {
            return false;
        }
    }

    private static final class FakeCreateCompanionNotificationUseCase
            implements CreateCompanionNotificationUseCase {

        private final List<CreateCompanionNotificationCommand> commands = new ArrayList<>();

        @Override
        public CompanionNotification create(final CreateCompanionNotificationCommand command) {
            commands.add(command);
            return new CompanionNotification(
                    1L,
                    command.recipientUserId(),
                    command.notificationType(),
                    command.targetType(),
                    command.targetId(),
                    null,
                    NOW
            );
        }
    }
}
