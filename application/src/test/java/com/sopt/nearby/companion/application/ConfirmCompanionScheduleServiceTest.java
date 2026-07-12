// 동행 일정 확정 서비스의 상태 전이 경쟁 조건 방지를 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompanionScheduleAlreadyConfirmedException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionScheduleException;
import com.sopt.nearby.companion.domain.exception.InvalidOpenChatUrlException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionSchedule;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceCache;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfirmCompanionScheduleServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 5, 12, 0);

    private FakeCompanionMatchRepository matchRepository;
    private FakeCompanionPostRepository postRepository;
    private FakeCompanionScheduleRepository scheduleRepository;
    private FakeCompanionMeetingRepository meetingRepository;
    private ConfirmCompanionScheduleService service;

    @BeforeEach
    void setUp() {
        matchRepository = new FakeCompanionMatchRepository();
        postRepository = new FakeCompanionPostRepository();
        scheduleRepository = new FakeCompanionScheduleRepository();
        meetingRepository = new FakeCompanionMeetingRepository();
        service = new ConfirmCompanionScheduleService(
                matchRepository,
                postRepository,
                scheduleRepository,
                meetingRepository,
                new FakeResolvePlaceCacheUseCase()
        );

        matchRepository.save(new CompanionMatch(1L, 10L, CompanionMatchStatus.MATCHED, NOW));
        postRepository.save(new CompanionPost(
                10L,
                7L,
                100L,
                CompanionPostMeetingTimeType.NOW,
                null,
                NOW.plusHours(1),
                4,
                false,
                "함께 식사해요.",
                "https://open.kakao.com/o/original",
                CompanionPostStatus.CLOSED,
                NOW
        ));
    }

    @Test
    void confirmsScheduleWhenRequesterIsPostHostAndMatchIsMatched() {
        ConfirmCompanionScheduleResult result = service.confirm(command());

        assertEquals(1L, result.matchId());
        assertEquals(1L, result.scheduleId());
        assertEquals(CompanionMatchStatus.SCHEDULE_CONFIRMED, result.matchStatus());
        assertEquals(1, matchRepository.confirmScheduleAttempts);
        assertEquals(1, scheduleRepository.saveCount);
        assertEquals(100L, scheduleRepository.schedules.get(1L).placeId());
        CompanionMeeting meeting = meetingRepository.meetings.get(1L);
        assertEquals(1L, meeting.matchId());
        assertEquals(CompanionMeetingStatus.ONGOING, meeting.status());
        assertEquals(NOW.plusDays(2), meeting.startedAt());
        assertEquals(null, meeting.completedAt());
        assertEquals(
                "https://open.kakao.com/o/confirmed",
                postRepository.posts.get(10L).openChatUrl()
        );
        assertEquals(CompanionPostMeetingTimeType.NOW, postRepository.posts.get(10L).meetingTimeType());
        assertEquals(null, postRepository.posts.get(10L).meetingAt());
        assertEquals(NOW.plusHours(1), postRepository.posts.get(10L).exposureExpiresAt());
        assertEquals(false, postRepository.posts.get(10L).departEvenIfNotFull());
    }

    @Test
    void throwsInvalidOpenChatUrlWhenUrlIsNotKakaoOpenChat() {
        assertThrows(
                InvalidOpenChatUrlException.class,
                () -> service.confirm(command("https://example.com/o/confirmed"))
        );
    }

    @Test
    void throwsInvalidOpenChatUrlWhenUrlIsEmpty() {
        assertThrows(
                InvalidOpenChatUrlException.class,
                () -> service.confirm(command(""))
        );
    }

    @Test
    void throwsForbiddenWhenRequesterIsNotPostHost() {
        assertThrows(
                ForbiddenCompanionScheduleException.class,
                () -> service.confirm(command(8L))
        );

        assertEquals(0, matchRepository.confirmScheduleAttempts);
        assertEquals(0, scheduleRepository.saveCount);
    }

    @Test
    void throwsAlreadyCanceledWhenMatchIsCanceled() {
        matchRepository.save(new CompanionMatch(1L, 10L, CompanionMatchStatus.CANCELED, NOW));

        assertThrows(
                CompanionMatchAlreadyCanceledException.class,
                () -> service.confirm(command())
        );

        assertEquals(0, matchRepository.confirmScheduleAttempts);
        assertEquals(0, scheduleRepository.saveCount);
    }

    @Test
    void throwsAlreadyCompletedWhenMatchIsCompleted() {
        matchRepository.save(new CompanionMatch(1L, 10L, CompanionMatchStatus.COMPLETED, NOW));

        assertThrows(
                CompanionMatchAlreadyCompletedException.class,
                () -> service.confirm(command())
        );

        assertEquals(0, matchRepository.confirmScheduleAttempts);
        assertEquals(0, scheduleRepository.saveCount);
    }

    @Test
    void throwsAlreadyConfirmedWhenMatchIsScheduleConfirmed() {
        matchRepository.save(new CompanionMatch(1L, 10L, CompanionMatchStatus.SCHEDULE_CONFIRMED, NOW));

        assertThrows(
                CompanionScheduleAlreadyConfirmedException.class,
                () -> service.confirm(command())
        );

        assertEquals(0, matchRepository.confirmScheduleAttempts);
        assertEquals(0, scheduleRepository.saveCount);
    }

    @Test
    void doesNotCreateScheduleWhenMatchWasAlreadyConfirmedByConcurrentRequest() {
        matchRepository.confirmScheduleResult = false;
        matchRepository.latestAfterFailedConfirm = new CompanionMatch(
                1L,
                10L,
                CompanionMatchStatus.SCHEDULE_CONFIRMED,
                NOW
        );

        assertThrows(
                CompanionScheduleAlreadyConfirmedException.class,
                () -> service.confirm(command())
        );

        assertEquals(0, scheduleRepository.saveCount);
        assertEquals(0, meetingRepository.meetings.size());
        assertEquals(1, matchRepository.confirmScheduleAttempts);
    }

    private ConfirmCompanionScheduleCommand command() {
        return command(7L);
    }

    private ConfirmCompanionScheduleCommand command(final Long requesterUserId) {
        return command(requesterUserId, "https://open.kakao.com/o/confirmed");
    }

    private ConfirmCompanionScheduleCommand command(final String openChatUrl) {
        return command(7L, openChatUrl);
    }

    private ConfirmCompanionScheduleCommand command(
            final Long requesterUserId,
            final String openChatUrl
    ) {
        return new ConfirmCompanionScheduleCommand(
                1L,
                requesterUserId,
                NOW.plusDays(2),
                new ConfirmCompanionScheduleCommand.Place(
                        "google-place-id",
                        "Siutat condal",
                        "Rambla de Catalunya, 16",
                        new BigDecimal("41.39020500"),
                        new BigDecimal("2.16354800")
                ),
                openChatUrl
        );
    }

    private static final class FakeCompanionMatchRepository implements CompanionMatchRepository {

        private final Map<Long, CompanionMatch> matches = new HashMap<>();
        private boolean confirmScheduleResult = true;
        private CompanionMatch latestAfterFailedConfirm;
        private int confirmScheduleAttempts;

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
            confirmScheduleAttempts++;
            if (confirmScheduleResult) {
                CompanionMatch match = matches.get(matchId);
                matches.put(matchId, new CompanionMatch(
                        match.id(),
                        match.postId(),
                        CompanionMatchStatus.SCHEDULE_CONFIRMED,
                        match.createdAt()
                ));
            }
            if (!confirmScheduleResult && latestAfterFailedConfirm != null) {
                matches.put(matchId, latestAfterFailedConfirm);
            }
            return confirmScheduleResult;
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
            CompanionSchedule saved = new CompanionSchedule(
                    1L,
                    model.matchId(),
                    model.placeId(),
                    model.scheduledAt(),
                    model.estimatedDurationMinutes(),
                    model.confirmed()
            );
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
                    .filter(schedule -> schedule.matchId().equals(matchId) && schedule.confirmed())
                    .findFirst();
        }
    }

    private static final class FakeCompanionMeetingRepository implements CompanionMeetingRepository {

        private final Map<Long, CompanionMeeting> meetings = new HashMap<>();

        @Override
        public CompanionMeeting save(final CompanionMeeting model) {
            CompanionMeeting saved = new CompanionMeeting(
                    1L,
                    model.matchId(),
                    model.status(),
                    model.startedAt(),
                    model.completedAt()
            );
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

    private static final class FakeResolvePlaceCacheUseCase implements ResolvePlaceCacheUseCase {

        @Override
        public ResolvedPlaceCache resolve(final ResolvePlaceCacheCommand command) {
            return new ResolvedPlaceCache(100L);
        }
    }
}
