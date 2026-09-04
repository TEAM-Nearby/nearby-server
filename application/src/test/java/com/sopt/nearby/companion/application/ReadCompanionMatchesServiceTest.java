// 동행 장소 도시를 기준으로 현재 현지 시각을 계산하는 조회 서비스를 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionMatchSummaryQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReadCompanionMatchesServiceTest {

    @Test
    void resolvesCityAndCurrentLocalTimeWithoutDroppingUnsupportedPlaces() {
        CompanionMatchSummaryQueryPort queryPort = new StubQueryPort(List.of(
                summary(1L, "마드리드 스페인"),
                summary(2L, "Rome, Italy")
        ));
        Clock clock = Clock.fixed(Instant.parse("2026-07-01T12:00:00Z"), ZoneOffset.UTC);
        ReadCompanionMatchesService service = new ReadCompanionMatchesService(queryPort, clock);

        List<ReadCompanionMatchResult> results = service.getMatches(7L);

        assertEquals(2, results.size());
        assertEquals(CompanionCity.MADRID, results.get(0).city());
        assertEquals("2026-07-01T14:00+02:00", results.get(0).currentLocalTime().toOffsetDateTime().toString());
        assertNull(results.get(1).city());
        assertNull(results.get(1).currentLocalTime());
    }

    private CompanionMatchSummary summary(final Long matchId, final String placeAddress) {
        return new CompanionMatchSummary(
                matchId,
                "호스트",
                null,
                UserGender.FEMALE,
                "식당",
                placeAddress,
                LocalDateTime.of(2026, 7, 1, 19, 0),
                CompanionPostMeetingTimeType.SCHEDULED,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                "동행을 구해요",
                CompanionMatchStatus.MATCHED
        );
    }

    private record StubQueryPort(
            List<CompanionMatchSummary> summaries
    ) implements CompanionMatchSummaryQueryPort {

        @Override
        public List<CompanionMatchSummary> findAllByParticipantUserId(final Long userId) {
            return summaries;
        }

        @Override
        public Optional<String> findPlaceNameByPlaceId(final Long placeId) {
            return Optional.empty();
        }
    }
}
