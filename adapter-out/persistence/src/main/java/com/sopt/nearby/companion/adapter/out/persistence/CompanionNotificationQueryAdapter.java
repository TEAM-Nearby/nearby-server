// 동행 알림 목록 조회 Query Port를 구현하는 Persistence Adapter
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionNotificationQueryJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionNotificationProjection;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationHostProfile;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationPageCursor;
import com.sopt.nearby.companion.port.out.CompanionNotificationPageRow;
import com.sopt.nearby.companion.port.out.CompanionNotificationQueryPort;
import com.sopt.nearby.companion.port.out.CompanionPushContentQueryPort;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionNotificationQueryAdapter implements CompanionNotificationQueryPort, CompanionPushContentQueryPort {

    private final CompanionNotificationQueryJpaRepository repository;

    public CompanionNotificationQueryAdapter(final CompanionNotificationQueryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> findRecipientNickname(final Long recipientUserId) {
        return repository.findRecipientNickname(recipientUserId);
    }

    @Override
    public Optional<String> findPlaceNameByApplicationId(final Long applicationId) {
        return repository.findPlaceNameByApplicationId(applicationId);
    }

    @Override
    public List<CompanionNotificationSummary> findAllByUserIdAndDirection(
            final Long userId,
            final CompanionNotificationDirection direction
    ) {
        List<CompanionNotificationProjection> rows = switch (direction) {
            case SENT -> repository.findSentByUserId(userId);
            case RECEIVED -> repository.findReceivedByUserId(userId);
        };

        return rows.stream()
                .map(row -> toSummary(direction, row))
                .toList();
    }

    @Override
    public List<CompanionNotificationPageRow> findPageByUserIdAndDirection(
            final Long userId,
            final CompanionNotificationDirection direction,
            final int limit,
            final CompanionNotificationPageCursor cursor
    ) {
        LocalDateTime cursorCreatedAt = cursor == null ? null : cursor.createdAt();
        Long cursorId = cursor == null ? null : cursor.notificationId();
        List<CompanionNotificationProjection> rows = switch (direction) {
            case SENT -> repository.findSentPage(userId, cursorCreatedAt, cursorId, limit);
            case RECEIVED -> repository.findReceivedPage(userId, cursorCreatedAt, cursorId, limit);
        };

        return rows.stream()
                .map(row -> new CompanionNotificationPageRow(
                        row.getNotificationId(),
                        row.getCreatedAt(),
                        toSummary(direction, row)
                ))
                .toList();
    }

    private CompanionNotificationSummary toSummary(
            final CompanionNotificationDirection direction,
            final CompanionNotificationProjection row
    ) {
        return CompanionNotificationSummary.of(
                direction,
                row.getNotificationId(),
                row.getApplicationId(),
                CompanionApplicationStatus.valueOf(row.getApplicationStatus()),
                new CompanionNotificationHostProfile(
                        row.getHostUserId(),
                        row.getHostProfileImageUrl(),
                        row.getHostNickname()
                ),
                row.getPlaceName(),
                row.getMeetingAt(),
                row.getMatchId(),
                Boolean.TRUE.equals(row.getReadStatus())
        );
    }
}
