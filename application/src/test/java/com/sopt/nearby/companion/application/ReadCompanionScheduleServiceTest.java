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
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionScheduleDetail;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionScheduleDetailQueryPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadCompanionScheduleServiceTest {

    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2026, 7, 5, 18, 30);

    private FakeCompanionScheduleDetailQueryPort queryPort;
    private FakeCompanionMatchParticipantRepository participantRepository;
    private ReadCompanionScheduleService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeCompanionScheduleDetailQueryPort();
        participantRepository = new FakeCompanionMatchParticipantRepository();
        service = new ReadCompanionScheduleService(queryPort, participantRepository);
    }

    @Test
    void returnsScheduleDetailWhenRequesterIsMatchParticipant() {
        queryPort.save(confirmedScheduleDetail());
        participantRepository.addParticipant(1L, 7L);

        CompanionScheduleDetail result = service.getSchedule(1L, 7L);

        assertEquals(1L, result.matchId());
        assertEquals(CompanionMatchStatus.SCHEDULE_CONFIRMED, result.matchStatus());
        assertEquals(100L, result.schedule().scheduleId());
        assertEquals("google-place-id", result.schedule().place().googlePlaceId());
        assertEquals("Siutat condal", result.schedule().place().name());
        assertEquals("Rambla de Catalunya, 16", result.schedule().place().address());
        assertEquals(new BigDecimal("41.39020500"), result.schedule().place().latitude());
        assertEquals(new BigDecimal("2.16354800"), result.schedule().place().longitude());
        assertEquals(SCHEDULED_AT, result.schedule().scheduledAt());
        assertEquals("https://open.kakao.com/o/confirmed", result.openChatUrl());
    }

    @Test
    void returnsMatchedDetailWithNullScheduleWhenScheduleIsNotConfirmed() {
        queryPort.save(new CompanionScheduleDetail(
                1L,
                CompanionMatchStatus.MATCHED,
                null,
                null
        ));
        participantRepository.addParticipant(1L, 7L);

        CompanionScheduleDetail result = service.getSchedule(1L, 7L);

        assertEquals(CompanionMatchStatus.MATCHED, result.matchStatus());
        assertNull(result.schedule());
        assertNull(result.openChatUrl());
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
        queryPort.save(confirmedScheduleDetail());

        assertThrows(
                ForbiddenReadCompanionScheduleException.class,
                () -> service.getSchedule(1L, 7L)
        );
    }

    @Test
    void throwsScheduleNotReadableWhenMatchIsCanceled() {
        queryPort.save(new CompanionScheduleDetail(
                1L,
                CompanionMatchStatus.CANCELED,
                null,
                null
        ));
        participantRepository.addParticipant(1L, 7L);

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
                null
        ));
        participantRepository.addParticipant(1L, 7L);

        assertThrows(
                CompletedCompanionScheduleNotReadableException.class,
                () -> service.getSchedule(1L, 7L)
        );
    }

    private CompanionScheduleDetail confirmedScheduleDetail() {
        return new CompanionScheduleDetail(
                1L,
                CompanionMatchStatus.SCHEDULE_CONFIRMED,
                new CompanionScheduleDetail.Schedule(
                        100L,
                        new CompanionScheduleDetail.Place(
                                "google-place-id",
                                "Siutat condal",
                                "Rambla de Catalunya, 16",
                                new BigDecimal("41.39020500"),
                                new BigDecimal("2.16354800")
                        ),
                        SCHEDULED_AT
                ),
                "https://open.kakao.com/o/confirmed"
        );
    }

    private static final class FakeCompanionScheduleDetailQueryPort implements CompanionScheduleDetailQueryPort {

        private final Map<Long, CompanionScheduleDetail> scheduleDetails = new HashMap<>();

        private void save(final CompanionScheduleDetail scheduleDetail) {
            scheduleDetails.put(scheduleDetail.matchId(), scheduleDetail);
        }

        @Override
        public Optional<CompanionScheduleDetail> findByMatchId(final Long matchId) {
            return Optional.ofNullable(scheduleDetails.get(matchId));
        }
    }

    private static final class FakeCompanionMatchParticipantRepository
            implements CompanionMatchParticipantRepository {

        private final Map<Long, List<Long>> participantUserIds = new HashMap<>();

        private void addParticipant(final Long matchId, final Long userId) {
            participantUserIds.put(matchId, List.of(userId));
        }

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
            return List.of();
        }

        @Override
        public boolean existsByMatchIdAndUserId(final Long matchId, final Long userId) {
            return participantUserIds.getOrDefault(matchId, List.of()).contains(userId);
        }
    }
}
