// 내 동행 일정 조회 서비스의 권한 검증과 상태별 예외 처리를 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchScheduleNotReadableException;
import com.sopt.nearby.companion.domain.exception.CompletedCompanionScheduleNotReadableException;
import com.sopt.nearby.companion.domain.exception.ForbiddenReadCompanionScheduleException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMatchIdException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionScheduleDetail;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.port.out.CompanionScheduleDetailQueryPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadCompanionScheduleServiceTest {

    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 7, 5, 18, 30);
    private static final LocalDateTime EXPOSURE_EXPIRES_AT = LocalDateTime.of(2026, 7, 5, 13, 0);

    private FakeCompanionScheduleDetailQueryPort queryPort;
    private ReadCompanionScheduleService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeCompanionScheduleDetailQueryPort();
        service = new ReadCompanionScheduleService(queryPort);
    }

    @Test
    void returnsScheduleDetailWhenRequesterIsMatchParticipant() {
        queryPort.save(confirmedScheduleDetail());

        CompanionScheduleDetail result = service.getSchedule(1L, 7L);

        assertEquals(1L, result.matchId());
        assertEquals(CompanionMatchStatus.SCHEDULE_CONFIRMED, result.matchStatus());
        assertEquals("google-place-id", result.schedule().place().googlePlaceId());
        assertEquals("Siutat condal", result.schedule().place().name());
        assertEquals("Rambla de Catalunya, 16", result.schedule().place().address());
        assertEquals(new BigDecimal("41.39020500"), result.schedule().place().latitude());
        assertEquals(new BigDecimal("2.16354800"), result.schedule().place().longitude());
        assertEquals(SCHEDULED_AT, result.schedule().scheduledAt());
        assertEquals("https://open.kakao.com/o/confirmed", result.openChatUrl());
        assertEquals("루피", result.userNickname());
        assertEquals(CompanionPostMeetingTimeType.SCHEDULED, result.meetingTimeType());
        assertEquals(MatchParticipantRole.HOST, result.currentUserRole());
    }

    @Test
    void returnsNowScheduleConfirmedDetailWithScheduleAndOpenChatUrl() {
        queryPort.save(new CompanionScheduleDetail(
                1L,
                CompanionMatchStatus.SCHEDULE_CONFIRMED,
                new CompanionScheduleDetail.Schedule(
                        new CompanionScheduleDetail.Place(
                                "google-place-id",
                                "Siutat condal",
                                "Rambla de Catalunya, 16",
                                new BigDecimal("41.39020500"),
                                new BigDecimal("2.16354800")
                        ),
                        EXPOSURE_EXPIRES_AT
                ),
                "https://open.kakao.com/o/not-yet",
                "루피",
                CompanionPostMeetingTimeType.NOW,
                MatchParticipantRole.GUEST
        ));

        CompanionScheduleDetail result = service.getSchedule(1L, 7L);

        assertEquals(CompanionMatchStatus.SCHEDULE_CONFIRMED, result.matchStatus());
        assertEquals("google-place-id", result.schedule().place().googlePlaceId());
        assertEquals("Siutat condal", result.schedule().place().name());
        assertEquals(EXPOSURE_EXPIRES_AT, result.schedule().scheduledAt());
        assertEquals("https://open.kakao.com/o/not-yet", result.openChatUrl());
        assertEquals("루피", result.userNickname());
        assertEquals(CompanionPostMeetingTimeType.NOW, result.meetingTimeType());
        assertEquals(MatchParticipantRole.GUEST, result.currentUserRole());
    }

    @Test
    void throwsInvalidMatchIdWhenMatchIdIsNotPositive() {
        assertThrows(
                InvalidCompanionMatchIdException.class,
                () -> service.getSchedule(0L, 7L)
        );
    }

    @Test
    void throwsNotFoundWhenMatchDoesNotExist() {
        assertThrows(
                CompanionMatchNotFoundException.class,
                () -> service.getSchedule(999L, 7L)
        );
    }

    @Test
    void throwsForbiddenWhenRequesterIsNotMatchParticipant() {
        queryPort.save(confirmedScheduleDetail(null));

        ForbiddenReadCompanionScheduleException exception = assertThrows(
                ForbiddenReadCompanionScheduleException.class,
                () -> service.getSchedule(1L, 7L)
        );

        assertEquals("FORBIDDEN_READ_COMPANION_SCHEDULE", exception.getErrorCode().name());
        assertEquals("동행 일정을 조회할 권한이 없습니다.", exception.getErrorCode().message());
    }

    @Test
    void throwsScheduleNotReadableWhenMatchIsCanceled() {
        queryPort.save(new CompanionScheduleDetail(
                1L,
                CompanionMatchStatus.CANCELED,
                null,
                null,
                "루피",
                CompanionPostMeetingTimeType.SCHEDULED,
                MatchParticipantRole.HOST
        ));

        assertThrows(
                CompanionMatchScheduleNotReadableException.class,
                () -> service.getSchedule(1L, 7L)
        );
    }

    @Test
    void throwsCompletedScheduleNotReadableWhenMatchIsCompleted() {
        queryPort.save(new CompanionScheduleDetail(
                1L,
                CompanionMatchStatus.COMPLETED,
                null,
                null,
                "루피",
                CompanionPostMeetingTimeType.SCHEDULED,
                MatchParticipantRole.HOST
        ));

        assertThrows(
                CompletedCompanionScheduleNotReadableException.class,
                () -> service.getSchedule(1L, 7L)
        );
    }

    private CompanionScheduleDetail confirmedScheduleDetail() {
        return confirmedScheduleDetail(MatchParticipantRole.HOST);
    }

    private CompanionScheduleDetail confirmedScheduleDetail(final MatchParticipantRole currentUserRole) {
        return new CompanionScheduleDetail(
                1L,
                CompanionMatchStatus.SCHEDULE_CONFIRMED,
                new CompanionScheduleDetail.Schedule(
                        new CompanionScheduleDetail.Place(
                                "google-place-id",
                                "Siutat condal",
                                "Rambla de Catalunya, 16",
                                new BigDecimal("41.39020500"),
                                new BigDecimal("2.16354800")
                        ),
                        SCHEDULED_AT
                ),
                "https://open.kakao.com/o/confirmed",
                "루피",
                CompanionPostMeetingTimeType.SCHEDULED,
                currentUserRole
        );
    }

    private static final class FakeCompanionScheduleDetailQueryPort implements CompanionScheduleDetailQueryPort {

        private final Map<Long, CompanionScheduleDetail> scheduleDetails = new HashMap<>();

        private void save(final CompanionScheduleDetail scheduleDetail) {
            scheduleDetails.put(scheduleDetail.matchId(), scheduleDetail);
        }

        @Override
        public Optional<CompanionScheduleDetail> findByMatchIdAndUserId(final Long matchId, final Long userId) {
            return Optional.ofNullable(scheduleDetails.get(matchId));
        }
    }

}
