// 동행 만남 인증 서비스의 검증, 거리 계산, 멱등 처리를 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CheckInTimeNotAllowedException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionScheduleNotConfirmedException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.InvalidCheckInRequestException;
import com.sopt.nearby.companion.domain.exception.OutOfCheckInRadiusException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingCheckInContext;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingCheckInQueryPort;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckInCompanionMeetingServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-29T18:35:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 18, 35);
    private static final Long MEETING_ID = 1L;
    private static final Long MATCH_ID = 10L;
    private static final Long USER_ID = 7L;

    private FakeCompanionMeetingCheckInQueryPort queryPort;
    private FakeCompanionMatchParticipantRepository participantRepository;
    private FakeMeetingCheckInRepository checkInRepository;
    private CheckInCompanionMeetingService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeCompanionMeetingCheckInQueryPort();
        participantRepository = new FakeCompanionMatchParticipantRepository();
        checkInRepository = new FakeMeetingCheckInRepository();
        service = new CheckInCompanionMeetingService(queryPort, participantRepository, checkInRepository, CLOCK);

        queryPort.put(context(CompanionMeetingStatus.ONGOING, NOW.minusMinutes(5)));
        participantRepository.put(MATCH_ID, List.of(
                participant(USER_ID, MatchParticipantRole.GUEST),
                participant(8L, MatchParticipantRole.HOST)
        ));
    }

    @Test
    void checksInWithinAllowedTimeAndRadius() {
        CheckInCompanionMeetingResult result = service.checkIn(command("41.39020500", "2.16354800"));

        assertEquals(MEETING_ID, result.meetingId());
        assertEquals(CompanionMeetingStatus.ONGOING, result.meetingStatus());
        assertEquals(true, result.currentUserCheckedIn());
        assertEquals(1L, result.checkedInCount());
        assertEquals(2L, result.totalParticipantCount());
        assertEquals(false, result.allParticipantsCheckedIn());
        assertEquals(false, result.canMoveToComplete());
        assertEquals(NOW, result.checkedInAt());
        assertEquals(0.0, result.distanceMeters());
        assertEquals(150.0, result.allowedRadiusMeters());
        assertEquals(LocalDateTime.of(2026, 6, 29, 17, 30), result.checkInAvailableFrom());
        assertEquals(LocalDateTime.of(2026, 6, 29, 19, 30), result.checkInAvailableUntil());
        assertEquals(false, result.alreadyCompleted());
    }

    @Test
    void returnsAlreadyCompletedWithoutRecheckingTimeAndRadius() {
        LocalDateTime firstCheckedInAt = NOW.minusMinutes(30);
        checkInRepository.put(new MeetingCheckIn(
                100L,
                MEETING_ID,
                USER_ID,
                new BigDecimal("41.39020500"),
                new BigDecimal("2.16354800"),
                firstCheckedInAt,
                null
        ));
        queryPort.put(context(CompanionMeetingStatus.ONGOING, NOW.minusDays(1)));

        CheckInCompanionMeetingResult result = service.checkIn(command("40.00000000", "2.00000000"));

        assertEquals(true, result.currentUserCheckedIn());
        assertEquals(true, result.alreadyCompleted());
        assertEquals(firstCheckedInAt, result.checkedInAt());
        assertEquals(0, checkInRepository.saveIfAbsentCount);
    }

    @Test
    void returnsAllParticipantsCheckedInWhenEveryParticipantHasCheckedIn() {
        checkInRepository.put(new MeetingCheckIn(
                100L,
                MEETING_ID,
                8L,
                new BigDecimal("41.39020500"),
                new BigDecimal("2.16354800"),
                NOW.minusMinutes(10),
                null
        ));

        CheckInCompanionMeetingResult result = service.checkIn(command("41.39020500", "2.16354800"));

        assertEquals(2L, result.checkedInCount());
        assertEquals(2L, result.totalParticipantCount());
        assertEquals(true, result.allParticipantsCheckedIn());
        assertEquals(true, result.canMoveToComplete());
    }

    @Test
    void rejectsInvalidRequestValues() {
        assertThrows(InvalidCheckInRequestException.class, () -> service.checkIn(null));
        assertThrows(InvalidCheckInRequestException.class, () -> service.checkIn(new CheckInCompanionMeetingCommand(
                USER_ID,
                0L,
                new BigDecimal("41.39020500"),
                new BigDecimal("2.16354800")
        )));
        assertThrows(InvalidCheckInRequestException.class, () -> service.checkIn(command("91.00000000", "2.0")));
        assertThrows(InvalidCheckInRequestException.class, () -> service.checkIn(command("41.0", "181.0")));
    }

    @Test
    void rejectsMissingMeetingAndForbiddenParticipant() {
        queryPort.contexts.clear();

        assertThrows(CompanionMeetingNotFoundException.class, () -> service.checkIn(command("41.39020500", "2.0")));

        queryPort.put(context(CompanionMeetingStatus.ONGOING, NOW.minusMinutes(5)));
        participantRepository.put(MATCH_ID, List.of(participant(99L, MatchParticipantRole.HOST)));

        assertThrows(ForbiddenCompanionMeetingException.class, () -> service.checkIn(command("41.39020500", "2.0")));
    }

    @Test
    void rejectsCanceledMeetingAndMissingConfirmedSchedule() {
        queryPort.put(context(CompanionMeetingStatus.CANCELED, NOW.minusMinutes(5)));

        assertThrows(CompanionMeetingAlreadyCanceledException.class, () -> service.checkIn(command("41.39020500", "2.0")));

        queryPort.put(new CompanionMeetingCheckInContext(
                MEETING_ID,
                MATCH_ID,
                CompanionMeetingStatus.ONGOING,
                null,
                null,
                null,
                null,
                null
        ));

        assertThrows(CompanionScheduleNotConfirmedException.class, () -> service.checkIn(command("41.39020500", "2.0")));
    }

    @Test
    void rejectsNewCheckInOutsideAllowedTimeOrRadius() {
        queryPort.put(context(CompanionMeetingStatus.ONGOING, NOW.plusHours(3)));

        assertThrows(CheckInTimeNotAllowedException.class, () -> service.checkIn(command("41.39020500", "2.16354800")));

        queryPort.put(context(CompanionMeetingStatus.ONGOING, NOW.minusMinutes(5)));

        assertThrows(OutOfCheckInRadiusException.class, () -> service.checkIn(command("40.00000000", "2.00000000")));
    }

    @Test
    void rejectsAntipodalCoordinatesWhenHaversineRoundingExceedsOne() {
        assertThrows(
                OutOfCheckInRadiusException.class,
                () -> service.checkIn(command("-41.39020546", "-177.83645300"))
        );
    }

    private CheckInCompanionMeetingCommand command(final String latitude, final String longitude) {
        return new CheckInCompanionMeetingCommand(
                USER_ID,
                MEETING_ID,
                new BigDecimal(latitude),
                new BigDecimal(longitude)
        );
    }

    private CompanionMeetingCheckInContext context(
            final CompanionMeetingStatus meetingStatus,
            final LocalDateTime scheduledAt
    ) {
        return new CompanionMeetingCheckInContext(
                MEETING_ID,
                MATCH_ID,
                meetingStatus,
                20L,
                30L,
                scheduledAt,
                new BigDecimal("41.39020500"),
                new BigDecimal("2.16354800")
        );
    }

    private CompanionMatchParticipant participant(final Long userId, final MatchParticipantRole role) {
        return new CompanionMatchParticipant(null, MATCH_ID, userId, null, role);
    }

    private static final class FakeCompanionMeetingCheckInQueryPort implements CompanionMeetingCheckInQueryPort {

        private final Map<Long, CompanionMeetingCheckInContext> contexts = new HashMap<>();

        @Override
        public Optional<CompanionMeetingCheckInContext> findByMeetingId(final Long meetingId) {
            return Optional.ofNullable(contexts.get(meetingId));
        }

        private void put(final CompanionMeetingCheckInContext context) {
            contexts.put(context.meetingId(), context);
        }
    }

    private static final class FakeCompanionMatchParticipantRepository
            implements CompanionMatchParticipantRepository {

        private final Map<Long, List<CompanionMatchParticipant>> participants = new HashMap<>();

        @Override
        public CompanionMatchParticipant save(final CompanionMatchParticipant model) {
            participants.computeIfAbsent(model.matchId(), ignored -> new ArrayList<>()).add(model);
            return model;
        }

        @Override
        public Optional<CompanionMatchParticipant> findById(final Long id) {
            return participants.values().stream()
                    .flatMap(List::stream)
                    .filter(participant -> participant.id() != null && participant.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<CompanionMatchParticipant> findAllByMatchId(final Long matchId) {
            return participants.getOrDefault(matchId, List.of());
        }

        @Override
        public boolean existsByMatchIdAndUserId(final Long matchId, final Long userId) {
            return findAllByMatchId(matchId).stream()
                    .anyMatch(participant -> participant.userId().equals(userId));
        }

        private void put(final Long matchId, final List<CompanionMatchParticipant> matchParticipants) {
            participants.put(matchId, matchParticipants);
        }
    }

    private static final class FakeMeetingCheckInRepository implements MeetingCheckInRepository {

        private final Map<Key, MeetingCheckIn> checkIns = new HashMap<>();
        private long nextId = 1L;
        private int saveIfAbsentCount = 0;

        @Override
        public MeetingCheckIn save(final MeetingCheckIn model) {
            MeetingCheckIn saved = withId(model);
            checkIns.put(new Key(saved.meetingId(), saved.userId()), saved);
            return saved;
        }

        @Override
        public Optional<MeetingCheckIn> findById(final Long id) {
            return checkIns.values().stream()
                    .filter(checkIn -> checkIn.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<MeetingCheckIn> findByMeetingIdAndUserId(final Long meetingId, final Long userId) {
            return Optional.ofNullable(checkIns.get(new Key(meetingId, userId)));
        }

        @Override
        public long countByMeetingId(final Long meetingId) {
            return checkIns.values().stream()
                    .filter(checkIn -> checkIn.meetingId().equals(meetingId))
                    .count();
        }

        @Override
        public long countCompletedByMeetingId(final Long meetingId) {
            return checkIns.values().stream()
                    .filter(checkIn -> checkIn.meetingId().equals(meetingId))
                    .filter(checkIn -> checkIn.completedAt() != null)
                    .count();
        }

        @Override
        public MeetingCheckIn saveIfAbsent(final MeetingCheckIn checkIn) {
            saveIfAbsentCount++;
            return findByMeetingIdAndUserId(checkIn.meetingId(), checkIn.userId())
                    .orElseGet(() -> save(checkIn));
        }

        private void put(final MeetingCheckIn checkIn) {
            save(checkIn);
        }

        private MeetingCheckIn withId(final MeetingCheckIn model) {
            if (model.id() != null) {
                return model;
            }
            return new MeetingCheckIn(
                    nextId++,
                    model.meetingId(),
                    model.userId(),
                    model.latitude(),
                    model.longitude(),
                    model.checkedInAt(),
                    model.completedAt()
            );
        }

        private record Key(Long meetingId, Long userId) {
        }
    }
}
