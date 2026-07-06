// 동행 알림 목록 조회 서비스의 Query Port 위임을 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationHostProfile;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import com.sopt.nearby.companion.port.out.CompanionNotificationQueryPort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReadCompanionNotificationsServiceTest {

    @Test
    void delegatesUserIdAndDirectionToQueryPort() {
        FakeCompanionNotificationQueryPort queryPort = new FakeCompanionNotificationQueryPort();
        ReadCompanionNotificationsService service = new ReadCompanionNotificationsService(queryPort);
        queryPort.result = List.of(CompanionNotificationSummary.of(
                CompanionNotificationDirection.SENT,
                99L,
                1L,
                CompanionApplicationStatus.ACCEPTED,
                new CompanionNotificationHostProfile(100L, null, "호스트"),
                "오노테라",
                LocalDateTime.of(2026, 6, 18, 18, 30),
                10L,
                false
        ));

        List<CompanionNotificationSummary> result = service.getNotifications(
                7L,
                CompanionNotificationDirection.SENT
        );

        assertEquals(7L, queryPort.userId);
        assertEquals(CompanionNotificationDirection.SENT, queryPort.direction);
        assertEquals(queryPort.result, result);
    }

    private static final class FakeCompanionNotificationQueryPort implements CompanionNotificationQueryPort {

        private List<CompanionNotificationSummary> result = List.of();
        private Long userId;
        private CompanionNotificationDirection direction;

        @Override
        public List<CompanionNotificationSummary> findAllByUserIdAndDirection(
                final Long userId,
                final CompanionNotificationDirection direction
        ) {
            this.userId = userId;
            this.direction = direction;
            return result;
        }
    }
}
