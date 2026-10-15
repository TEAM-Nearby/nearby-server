// 진행 중인 동행 목록 조회 서비스가 조회 포트에 사용자 ID를 전달하는지 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingProgressStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingHostProfile;
import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingSummary;
import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.OngoingCompanionMeetingQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReadOngoingCompanionMeetingsServiceTest {
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-01T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void delegatesToQueryPortWithUserId() {
        FakeOngoingCompanionMeetingQueryPort queryPort = new FakeOngoingCompanionMeetingQueryPort();
        queryPort.result = List.of(summary(LocalDateTime.of(2026, 7, 1, 14, 0), false));
        ReadOngoingCompanionMeetingsService service = new ReadOngoingCompanionMeetingsService(queryPort, CLOCK);

        List<OngoingCompanionMeetingSummary> result = service.getOngoingMeetings(7L);

        assertEquals(7L, queryPort.userId);
        assertEquals(1, result.size());
        assertEquals(UserGender.FEMALE, result.getFirst().companion().gender());
        assertEquals(CompanionCity.MADRID, result.getFirst().city());
        assertEquals("2026-07-01T14:00+02:00", result.getFirst().currentLocalTime().toOffsetDateTime().toString());
    }

    @Test
    void excludesUnverifiedMeetingAfterCheckInDeadline() {
        FakeOngoingCompanionMeetingQueryPort queryPort = new FakeOngoingCompanionMeetingQueryPort();
        queryPort.result = List.of(summary(LocalDateTime.of(2026, 7, 1, 10, 59), false));
        ReadOngoingCompanionMeetingsService service = new ReadOngoingCompanionMeetingsService(queryPort, CLOCK);

        List<OngoingCompanionMeetingSummary> result = service.getOngoingMeetings(7L);

        assertEquals(List.of(), result);
    }

    @Test
    void keepsUnverifiedScheduledMeetingAtLocalCheckInDeadline() {
        FakeOngoingCompanionMeetingQueryPort queryPort = new FakeOngoingCompanionMeetingQueryPort();
        queryPort.result = List.of(summary(LocalDateTime.of(2026, 7, 1, 13, 0), false));
        ReadOngoingCompanionMeetingsService service = new ReadOngoingCompanionMeetingsService(queryPort, CLOCK);

        List<OngoingCompanionMeetingSummary> result = service.getOngoingMeetings(7L);

        assertEquals(1, result.size());
    }

    @Test
    void keepsUnverifiedMeetingBeforeCheckInDeadline() {
        FakeOngoingCompanionMeetingQueryPort queryPort = new FakeOngoingCompanionMeetingQueryPort();
        queryPort.result = List.of(summary(LocalDateTime.of(2026, 7, 1, 14, 0), false));
        ReadOngoingCompanionMeetingsService service = new ReadOngoingCompanionMeetingsService(queryPort, CLOCK);

        List<OngoingCompanionMeetingSummary> result = service.getOngoingMeetings(7L);

        assertEquals(1, result.size());
    }

    @Test
    void keepsSchedulingMeetingEvenWhenMeetingTimePassed() {
        FakeOngoingCompanionMeetingQueryPort queryPort = new FakeOngoingCompanionMeetingQueryPort();
        queryPort.result = List.of(summary(
                LocalDateTime.of(2026, 7, 1, 10, 59),
                false,
                CompanionMeetingProgressStatus.SCHEDULING
        ));
        ReadOngoingCompanionMeetingsService service = new ReadOngoingCompanionMeetingsService(queryPort, CLOCK);

        List<OngoingCompanionMeetingSummary> result = service.getOngoingMeetings(7L);

        assertEquals(1, result.size());
    }

    @Test
    void keepsMeetingWithoutMeetingTime() {
        FakeOngoingCompanionMeetingQueryPort queryPort = new FakeOngoingCompanionMeetingQueryPort();
        queryPort.result = List.of(summary(null, false));
        ReadOngoingCompanionMeetingsService service = new ReadOngoingCompanionMeetingsService(queryPort, CLOCK);

        List<OngoingCompanionMeetingSummary> result = service.getOngoingMeetings(7L);

        assertEquals(1, result.size());
    }

    @Test
    void keepsCheckedInMeetingAfterCheckInDeadline() {
        FakeOngoingCompanionMeetingQueryPort queryPort = new FakeOngoingCompanionMeetingQueryPort();
        queryPort.result = List.of(summary(LocalDateTime.of(2026, 7, 1, 10, 59), true));
        ReadOngoingCompanionMeetingsService service = new ReadOngoingCompanionMeetingsService(queryPort, CLOCK);

        List<OngoingCompanionMeetingSummary> result = service.getOngoingMeetings(7L);

        assertEquals(1, result.size());
    }

    @Test
    void excludesScheduledMeetingUsingPlaceLocalTime() {
        FakeOngoingCompanionMeetingQueryPort queryPort = new FakeOngoingCompanionMeetingQueryPort();
        queryPort.result = List.of(summary(LocalDateTime.of(2026, 7, 1, 12, 0), false));
        ReadOngoingCompanionMeetingsService service = new ReadOngoingCompanionMeetingsService(queryPort, CLOCK);

        List<OngoingCompanionMeetingSummary> result = service.getOngoingMeetings(7L);

        assertEquals(List.of(), result);
    }

    @Test
    void keepsNowMeetingUsingServerUtcTime() {
        FakeOngoingCompanionMeetingQueryPort queryPort = new FakeOngoingCompanionMeetingQueryPort();
        queryPort.result = List.of(summary(
                LocalDateTime.of(2026, 7, 1, 11, 0),
                false,
                CompanionMeetingProgressStatus.ONGOING,
                CompanionPostMeetingTimeType.NOW
        ));
        ReadOngoingCompanionMeetingsService service = new ReadOngoingCompanionMeetingsService(queryPort, CLOCK);

        List<OngoingCompanionMeetingSummary> result = service.getOngoingMeetings(7L);

        assertEquals(1, result.size());
    }

    private OngoingCompanionMeetingSummary summary(
            final LocalDateTime meetingAt,
            final boolean checkedIn
    ) {
        return summary(meetingAt, checkedIn, CompanionMeetingProgressStatus.ONGOING);
    }

    private OngoingCompanionMeetingSummary summary(
            final LocalDateTime meetingAt,
            final boolean checkedIn,
            final CompanionMeetingProgressStatus progressStatus
    ) {
        return new OngoingCompanionMeetingSummary(
                1L,
                10L,
                new OngoingCompanionMeetingHostProfile(7L, "https://image.url/profile.png", "정지영", UserGender.FEMALE),
                "시우다드 콘달",
                "Calle de Cuchilleros, 17, Madrid, Spain",
                null,
                null,
                meetingAt,
                CompanionPostMeetingTimeType.SCHEDULED,
                checkedIn,
                CompanionMeetingStatus.ONGOING,
                progressStatus
        );
    }

    private OngoingCompanionMeetingSummary summary(
            final LocalDateTime meetingAt,
            final boolean checkedIn,
            final CompanionMeetingProgressStatus progressStatus,
            final CompanionPostMeetingTimeType meetingTimeType
    ) {
        return new OngoingCompanionMeetingSummary(
                1L,
                10L,
                new OngoingCompanionMeetingHostProfile(7L, "https://image.url/profile.png", "정지영", UserGender.FEMALE),
                "시우다드 콘달",
                "Calle de Cuchilleros, 17, Madrid, Spain",
                null,
                null,
                meetingAt,
                meetingTimeType,
                checkedIn,
                CompanionMeetingStatus.ONGOING,
                progressStatus
        );
    }

    private static final class FakeOngoingCompanionMeetingQueryPort implements OngoingCompanionMeetingQueryPort {

        private Long userId;
        private List<OngoingCompanionMeetingSummary> result = List.of();

        @Override
        public List<OngoingCompanionMeetingSummary> findAllByParticipantUserId(final Long userId) {
            this.userId = userId;
            return result;
        }
    }
}
