// 동행 알림 커서 페이지 조회의 경계 조건과 사용자 범위를 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionNotificationCursorException;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationHostProfile;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationPageCursor;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import com.sopt.nearby.companion.port.out.CompanionNotificationPageRow;
import com.sopt.nearby.companion.port.out.CompanionNotificationQueryPort;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReadCompanionNotificationPageServiceTest {

    private static final LocalDateTime FIRST = LocalDateTime.of(2026, 9, 15, 0, 0);
    private static final LocalDateTime SECOND = FIRST.minusMinutes(1);

    @Test
    void requestsOneExtraRowAndBuildsCursorFromLastReturnedRow() {
        FakeQueryPort queryPort = new FakeQueryPort(List.of(
                row(10L, FIRST),
                row(9L, FIRST),
                row(8L, SECOND)
        ));
        ReadCompanionNotificationPageService service = new ReadCompanionNotificationPageService(queryPort);

        CompanionNotificationPage page = service.getPage(
                7L,
                CompanionNotificationDirection.RECEIVED,
                2,
                null
        );

        assertEquals(3, queryPort.limit);
        assertEquals(List.of(10L, 9L), page.requests().stream()
                .map(CompanionNotificationSummary::notificationId)
                .toList());
        assertEquals(CompanionNotificationPageCursor.of(
                CompanionNotificationDirection.RECEIVED, FIRST, 9L
        ).encode(), page.nextCursor());
        assertEquals(true, page.hasNext());
    }

    @Test
    void rejectsCursorForAnotherDirectionOrMalformedValue() {
        FakeQueryPort queryPort = new FakeQueryPort(List.of());
        ReadCompanionNotificationPageService service = new ReadCompanionNotificationPageService(queryPort);
        String sentCursor = CompanionNotificationPageCursor.of(
                CompanionNotificationDirection.SENT, FIRST, 10L
        ).encode();

        assertThrows(InvalidCompanionNotificationCursorException.class, () -> service.getPage(
                7L, CompanionNotificationDirection.RECEIVED, 20, sentCursor
        ));
        assertThrows(InvalidCompanionNotificationCursorException.class, () -> service.getPage(
                7L, CompanionNotificationDirection.RECEIVED, 20, "broken"
        ));
    }

    @Test
    void usesDefaultSizeAndReturnsNullCursorOnLastPage() {
        FakeQueryPort queryPort = new FakeQueryPort(List.of(row(1L, FIRST)));
        ReadCompanionNotificationPageService service = new ReadCompanionNotificationPageService(queryPort);

        CompanionNotificationPage page = service.getPage(
                7L, CompanionNotificationDirection.SENT, 0, null
        );

        assertEquals(21, queryPort.limit);
        assertNull(page.nextCursor());
        assertEquals(false, page.hasNext());
    }

    @Test
    void rejectsSizeOutsideSupportedRange() {
        ReadCompanionNotificationPageService service = new ReadCompanionNotificationPageService(new FakeQueryPort(List.of()));

        assertThrows(InvalidCompanionNotificationCursorException.class, () -> service.getPage(
                7L, CompanionNotificationDirection.SENT, 101, null
        ));
    }

    private CompanionNotificationPageRow row(final Long id, final LocalDateTime createdAt) {
        return new CompanionNotificationPageRow(id, createdAt, CompanionNotificationSummary.of(
                CompanionNotificationDirection.RECEIVED,
                id,
                id,
                CompanionApplicationStatus.PENDING,
                new CompanionNotificationHostProfile(99L, null, "호스트"),
                "장소",
                createdAt,
                null,
                false
        ));
    }

    private static final class FakeQueryPort implements CompanionNotificationQueryPort {

        private final List<CompanionNotificationPageRow> rows;
        private int limit;

        private FakeQueryPort(final List<CompanionNotificationPageRow> rows) {
            this.rows = rows;
        }

        @Override
        public List<CompanionNotificationSummary> findAllByUserIdAndDirection(
                final Long userId,
                final CompanionNotificationDirection direction
        ) {
            return List.of();
        }

        @Override
        public List<CompanionNotificationPageRow> findPageByUserIdAndDirection(
                final Long userId,
                final CompanionNotificationDirection direction,
                final int limit,
                final CompanionNotificationPageCursor cursor
        ) {
            this.limit = limit;
            return new ArrayList<>(rows);
        }
    }
}
