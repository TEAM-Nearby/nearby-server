// 동행 알림 커서 페이지 조회 유스케이스를 구현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionNotificationCursorException;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationPageCursor;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import com.sopt.nearby.companion.port.in.ReadCompanionNotificationPageUseCase;
import com.sopt.nearby.companion.port.out.CompanionNotificationPageRow;
import com.sopt.nearby.companion.port.out.CompanionNotificationQueryPort;
import java.util.List;

public class ReadCompanionNotificationPageService implements ReadCompanionNotificationPageUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final CompanionNotificationQueryPort queryPort;

    public ReadCompanionNotificationPageService(final CompanionNotificationQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public CompanionNotificationPage getPage(
            final Long userId,
            final CompanionNotificationDirection direction,
            final int size,
            final String cursor
    ) {
        if (userId == null || userId <= 0 || direction == null) {
            throw new InvalidCompanionNotificationCursorException();
        }
        int pageSize = size == 0 ? DEFAULT_SIZE : size;
        if (pageSize < 1 || pageSize > MAX_SIZE) {
            throw new InvalidCompanionNotificationCursorException();
        }

        CompanionNotificationPageCursor pageCursor = cursor == null || cursor.isBlank()
                ? null
                : CompanionNotificationPageCursor.decode(cursor);
        if (pageCursor != null && pageCursor.direction() != direction) {
            throw new InvalidCompanionNotificationCursorException();
        }

        List<CompanionNotificationPageRow> rows = queryPort.findPageByUserIdAndDirection(
                userId,
                direction,
                pageSize + 1,
                pageCursor
        );
        boolean hasNext = rows.size() > pageSize;
        List<CompanionNotificationPageRow> pageRows = hasNext ? rows.subList(0, pageSize) : rows;
        List<CompanionNotificationSummary> requests = pageRows.stream()
                .map(CompanionNotificationPageRow::summary)
                .toList();
        String nextCursor = hasNext
                ? CompanionNotificationPageCursor.of(
                        direction,
                        pageRows.get(pageRows.size() - 1).createdAt(),
                        pageRows.get(pageRows.size() - 1).notificationId()
                ).encode()
                : null;
        return new CompanionNotificationPage(requests, nextCursor, hasNext);
    }
}
