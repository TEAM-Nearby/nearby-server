// 동행 일정의 확정 및 시간 수정을 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionScheduleException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionScheduleRequestException;
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
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchSummaryQueryPort;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import com.sopt.nearby.companion.port.in.CreateCompanionNotificationUseCase;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfirmCompanionScheduleServiceTest {

    private static final LocalDateTime ORIGINAL_TIME = LocalDateTime.of(2026, 7, 5, 12, 0);
    private static final LocalDateTime UPDATED_TIME = ORIGINAL_TIME.plusDays(2);

    private FakeCompanionMatchRepository matchRepository;
    private FakeCompanionScheduleRepository scheduleRepository;
    private FakeCompanionMeetingRepository meetingRepository;
    private FakeCompanionMatchParticipantRepository participantRepository;
    private CapturingNotificationUseCase notificationUseCase;
    private ConfirmCompanionScheduleService service;
    private String placeAddress;
    private long checkedInCount;

    @BeforeEach
    void setUp() {
        matchRepository = new FakeCompanionMatchRepository();
        FakeCompanionPostRepository postRepository = new FakeCompanionPostRepository();
        scheduleRepository = new FakeCompanionScheduleRepository();
        meetingRepository = new FakeCompanionMeetingRepository();
        participantRepository = new FakeCompanionMatchParticipantRepository();
        notificationUseCase = new CapturingNotificationUseCase();
        service = new ConfirmCompanionScheduleService(
                matchRepository,
                postRepository,
                scheduleRepository,
                meetingRepository,
                participantRepository,
                notificationUseCase,
                new CompanionMatchSummaryQueryPort() {
                    public List<CompanionMatchSummary> findAllByParticipantUserId(Long userId) { return List.of(); }
                    public Optional<String> findPlaceNameByPlaceId(Long placeId) { return Optional.empty(); }
                    public Optional<String> findPlaceAddressByPlaceId(Long placeId) { return Optional.ofNullable(placeAddress); }
                },
                new MeetingCheckInRepository() {
                    public Optional<MeetingCheckIn> findById(Long id) { return Optional.empty(); }
                    public MeetingCheckIn save(MeetingCheckIn value) { return value; }
                    public Optional<MeetingCheckIn> findByMeetingIdAndUserId(Long meetingId, Long userId) { return Optional.empty(); }
                    public long countByMeetingId(Long meetingId) { return checkedInCount; }
                    public long countCompletedByMeetingId(Long meetingId) { return 0; }
                    public MeetingCheckIn saveIfAbsent(MeetingCheckIn value) { return value; }
                },
                Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC)
        );

        matchRepository.save(match(CompanionMatchStatus.SCHEDULE_CONFIRMED));
        postRepository.save(new CompanionPost(
                10L,
                7L,
                100L,
                CompanionPostMeetingTimeType.NOW,
                null,
                ORIGINAL_TIME.plusHours(1),
                4,
                false,
                "함께 식사해요.",
                "https://open.kakao.com/o/original",
                CompanionPostStatus.CLOSED,
                ORIGINAL_TIME
        ));
        scheduleRepository.save(new CompanionSchedule(1L, 1L, 100L, ORIGINAL_TIME, 60, true));
        scheduleRepository.saveCount = 0;
        participantRepository.save(new CompanionMatchParticipant(
                1L,
                1L,
                7L,
                null,
                MatchParticipantRole.HOST
        ));
        participantRepository.save(new CompanionMatchParticipant(
                2L,
                1L,
                8L,
                3L,
                MatchParticipantRole.GUEST
        ));
    }

    @Test
    void rejectsChangesAfterCheckIn() {
        meetingRepository.save(new CompanionMeeting(5L, 1L, CompanionMeetingStatus.ONGOING, ORIGINAL_TIME, null));
        checkedInCount = 1;
        assertThrows(InvalidCompanionScheduleRequestException.class, () -> service.update(command(7L, UPDATED_TIME)));
        assertEquals(0, scheduleRepository.saveCount);
    }

    @Test
    void savesParisScheduleInUtc() {
        placeAddress = "Paris, France";
        service.update(command(7L, UPDATED_TIME));
        assertEquals(UPDATED_TIME.minusHours(2), scheduleRepository.schedules.get(1L).scheduledAt());
        assertEquals(UPDATED_TIME, CompanionMeetingTimeConverter.utcToLocal(
                scheduleRepository.schedules.get(1L).scheduledAt(), java.time.ZoneId.of("Europe/Paris")));
    }

    @Test
    void rejectsPastScheduleAndAmbiguousLocalTime() {
        placeAddress = "Paris, France";
        assertThrows(InvalidCompanionScheduleRequestException.class,
                () -> service.update(command(7L, LocalDateTime.of(2026, 6, 30, 12, 0))));
        assertThrows(InvalidCompanionScheduleRequestException.class,
                () -> service.update(command(7L, LocalDateTime.of(2026, 10, 25, 2, 30))));
        assertEquals(0, scheduleRepository.saveCount);
    }

    @Test
    void updatesOnlyScheduledAtWhenRequesterIsHostAndScheduleIsConfirmed() {
        ConfirmCompanionScheduleResult result = service.update(command(7L, UPDATED_TIME));

        CompanionSchedule updated = scheduleRepository.schedules.get(1L);
        assertEquals(UPDATED_TIME, updated.scheduledAt());
        assertEquals(100L, updated.placeId());
        assertEquals(60, updated.estimatedDurationMinutes());
        assertEquals(true, updated.confirmed());
        assertEquals(1, scheduleRepository.saveCount);
        assertEquals(1L, result.matchId());
        assertEquals(1L, result.scheduleId());
        assertEquals(CompanionMatchStatus.SCHEDULE_CONFIRMED, result.matchStatus());
        assertEquals(1, matchRepository.findByIdForUpdateCalls);
        assertEquals(1, notificationUseCase.commands.size());
        assertEquals(8L, notificationUseCase.commands.get(0).recipientUserId());
        assertEquals(3L, notificationUseCase.commands.get(0).targetId());
        assertEquals(
                CompanionNotificationType.COMPANION_SCHEDULE_UPDATED,
                notificationUseCase.commands.get(0).notificationType()
        );
    }

    @Test
    void createsNewNotificationWheneverScheduleChangesAgain() {
        service.update(command(7L, UPDATED_TIME));
        service.update(command(7L, UPDATED_TIME.plusHours(1)));

        assertEquals(2, notificationUseCase.commands.size());
    }

    @Test
    void doesNotNotifyWhenScheduledAtIsUnchanged() {
        service.update(command(7L, ORIGINAL_TIME));

        assertEquals(0, notificationUseCase.commands.size());
    }

    @Test
    void confirmsMatchedScheduleUsingPostPlace() {
        matchRepository.save(match(CompanionMatchStatus.MATCHED));
        scheduleRepository.schedules.clear();

        ConfirmCompanionScheduleResult result = service.update(command(7L, UPDATED_TIME));

        CompanionSchedule schedule = scheduleRepository.findConfirmedByMatchId(1L).orElseThrow();
        CompanionMeeting meeting = meetingRepository.meetings.get(1L);
        assertEquals(CompanionMatchStatus.SCHEDULE_CONFIRMED, matchRepository.findById(1L).orElseThrow().status());
        assertEquals(100L, schedule.placeId());
        assertEquals(UPDATED_TIME, schedule.scheduledAt());
        assertEquals(CompanionMeetingStatus.ONGOING, meeting.status());
        assertEquals(UPDATED_TIME, meeting.startedAt());
        assertEquals(schedule.id(), result.scheduleId());
        assertEquals(CompanionMatchStatus.SCHEDULE_CONFIRMED, result.matchStatus());
        assertEquals(0, notificationUseCase.commands.size());
    }

    @Test
    void rejectsRequesterWhoIsNotHost() {
        assertThrows(
                ForbiddenCompanionScheduleException.class,
                () -> service.update(command(8L, UPDATED_TIME))
        );
        assertEquals(0, scheduleRepository.saveCount);
    }

    @Test
    void rejectsCompletedMatch() {
        matchRepository.save(match(CompanionMatchStatus.COMPLETED));
        assertThrows(
                InvalidCompanionScheduleRequestException.class,
                () -> service.update(command(7L, UPDATED_TIME))
        );
        assertEquals(0, scheduleRepository.saveCount);
    }

    @Test
    void keepsCanceledMatchConflict() {
        matchRepository.save(match(CompanionMatchStatus.CANCELED));

        assertThrows(
                CompanionMatchAlreadyCanceledException.class,
                () -> service.update(command(7L, UPDATED_TIME))
        );
        assertEquals(0, scheduleRepository.saveCount);
    }

    @Test
    void rejectsMissingScheduledAt() {
        assertThrows(
                InvalidCompanionScheduleRequestException.class,
                () -> service.update(command(7L, null))
        );
        assertEquals(0, scheduleRepository.saveCount);
    }

    private ConfirmCompanionScheduleCommand command(final Long requesterUserId, final LocalDateTime scheduledAt) {
        return new ConfirmCompanionScheduleCommand(1L, requesterUserId, scheduledAt);
    }

    private CompanionMatch match(final CompanionMatchStatus status) {
        return new CompanionMatch(1L, 10L, status, ORIGINAL_TIME);
    }

    private static final class FakeCompanionMatchRepository implements CompanionMatchRepository {

        private final Map<Long, CompanionMatch> matches = new HashMap<>();
        private int findByIdForUpdateCalls;

        @Override
        public CompanionMatch save(final CompanionMatch model) {
            matches.put(model.id(), model);
            return model;
        }

        @Override
        public Optional<CompanionMatch> findById(final Long id) {
            return Optional.ofNullable(matches.get(id));
        }

        @Override
        public Optional<CompanionMatch> findByIdForUpdate(final Long id) {
            findByIdForUpdateCalls++;
            return findById(id);
        }

        @Override
        public Optional<CompanionMatch> findFirstByPostIdAndStatus(
                final Long postId,
                final CompanionMatchStatus status
        ) {
            return Optional.empty();
        }

        @Override
        public boolean confirmScheduleIfMatched(final Long matchId) {
            CompanionMatch match = matches.get(matchId);
            if (match == null || match.status() != CompanionMatchStatus.MATCHED) {
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
    }

    private static final class FakeCompanionPostRepository implements CompanionPostRepository {

        private final Map<Long, CompanionPost> posts = new HashMap<>();

        @Override
        public CompanionPost save(final CompanionPost model) {
            posts.put(model.id(), model);
            return model;
        }

        @Override
        public Optional<CompanionPost> findById(final Long id) {
            return Optional.ofNullable(posts.get(id));
        }
    }

    private static final class FakeCompanionScheduleRepository implements CompanionScheduleRepository {

        private final Map<Long, CompanionSchedule> schedules = new HashMap<>();
        private long nextId = 2L;
        private int saveCount;

        @Override
        public CompanionSchedule save(final CompanionSchedule model) {
            saveCount++;
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
            return schedules.values().stream()
                    .filter(schedule -> schedule.matchId().equals(matchId) && schedule.confirmed())
                    .findFirst();
        }
    }

    private static final class FakeCompanionMeetingRepository implements CompanionMeetingRepository {

        @Override
        public Optional<CompanionMeeting> findByMatchId(final Long matchId) {
            return meetings.values().stream().filter(meeting -> meeting.matchId().equals(matchId)).findFirst();
        }


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

    private static final class FakeCompanionMatchParticipantRepository
            implements CompanionMatchParticipantRepository {

        private final Map<Long, CompanionMatchParticipant> participants = new HashMap<>();

        @Override
        public CompanionMatchParticipant save(final CompanionMatchParticipant model) {
            participants.put(model.id(), model);
            return model;
        }

        @Override
        public Optional<CompanionMatchParticipant> findById(final Long id) {
            return Optional.ofNullable(participants.get(id));
        }

        @Override
        public List<CompanionMatchParticipant> findAllByMatchId(final Long matchId) {
            return participants.values().stream()
                    .filter(participant -> participant.matchId().equals(matchId))
                    .toList();
        }

        @Override
        public boolean existsByMatchIdAndUserId(final Long matchId, final Long userId) {
            return participants.values().stream()
                    .anyMatch(participant -> participant.matchId().equals(matchId)
                            && participant.userId().equals(userId));
        }
    }

    private static final class CapturingNotificationUseCase implements CreateCompanionNotificationUseCase {

        private final List<CreateCompanionNotificationCommand> commands = new ArrayList<>();

        @Override
        public CompanionNotification create(final CreateCompanionNotificationCommand command) {
            commands.add(command);
            return new CompanionNotification(
                    (long) commands.size(),
                    command.recipientUserId(),
                    command.notificationType(),
                    command.targetType(),
                    command.targetId(),
                    null,
                    ORIGINAL_TIME
            );
        }
    }
}
