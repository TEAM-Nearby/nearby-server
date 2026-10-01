// 진행 중인 동행 목록 조회 서비스가 조회 포트에 사용자 ID를 전달하는지 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        queryPort.result = List.of(summary());
        ReadOngoingCompanionMeetingsService service = new ReadOngoingCompanionMeetingsService(queryPort, CLOCK);

        List<OngoingCompanionMeetingSummary> result = service.getOngoingMeetings(7L);

        assertEquals(7L, queryPort.userId);
        assertEquals(1, result.size());
        assertEquals(UserGender.FEMALE, result.getFirst().companion().gender());
        assertEquals(CompanionCity.MADRID, result.getFirst().city());
        assertEquals("2026-07-01T14:00+02:00", result.getFirst().currentLocalTime().toOffsetDateTime().toString());
    }

    private OngoingCompanionMeetingSummary summary() {
        return new OngoingCompanionMeetingSummary(
                1L,
                10L,
                new OngoingCompanionMeetingHostProfile(7L, "https://image.url/profile.png", "정지영", UserGender.FEMALE),
                "시우다드 콘달",
                "Calle de Cuchilleros, 17, Madrid, Spain",
                null,
                null,
                LocalDateTime.of(2026, 6, 29, 16, 30),
                CompanionPostMeetingTimeType.SCHEDULED,
                false,
                CompanionMeetingStatus.ONGOING,
                com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingProgressStatus.ONGOING
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
