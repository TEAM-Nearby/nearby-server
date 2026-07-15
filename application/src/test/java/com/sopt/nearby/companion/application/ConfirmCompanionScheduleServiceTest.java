// 확정된 동행 일정의 시간만 수정하는 서비스를 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionScheduleException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionScheduleRequestException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionSchedule;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import java.time.LocalDateTime;
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
    private ConfirmCompanionScheduleService service;

    @BeforeEach
    void setUp() {
        matchRepository = new FakeCompanionMatchRepository();
        FakeCompanionPostRepository postRepository = new FakeCompanionPostRepository();
        scheduleRepository = new FakeCompanionScheduleRepository();
        service = new ConfirmCompanionScheduleService(matchRepository, postRepository, scheduleRepository);

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
    void rejectsMatchThatIsNotScheduleConfirmedWithoutCompletedMatchException() {
        for (CompanionMatchStatus status : new CompanionMatchStatus[]{
                CompanionMatchStatus.MATCHED,
                CompanionMatchStatus.COMPLETED
        }) {
            matchRepository.save(match(status));
            assertThrows(
                    InvalidCompanionScheduleRequestException.class,
                    () -> service.update(command(7L, UPDATED_TIME))
            );
        }
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
        public Optional<CompanionMatch> findFirstByPostIdAndStatus(
                final Long postId,
                final CompanionMatchStatus status
        ) {
            return Optional.empty();
        }

        @Override
        public boolean confirmScheduleIfMatched(final Long matchId) {
            return false;
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
        private int saveCount;

        @Override
        public CompanionSchedule save(final CompanionSchedule model) {
            saveCount++;
            schedules.put(model.id(), model);
            return model;
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
}
