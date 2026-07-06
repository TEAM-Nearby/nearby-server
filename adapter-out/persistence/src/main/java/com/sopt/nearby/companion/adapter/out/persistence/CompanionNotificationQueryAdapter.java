// 동행 알림 목록 조회 Query Port를 구현하는 Persistence Adapter
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionNotificationQueryJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionNotificationProjection;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationHostProfile;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import com.sopt.nearby.companion.port.out.CompanionNotificationQueryPort;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionNotificationQueryAdapter implements CompanionNotificationQueryPort {

    private final CompanionNotificationQueryJpaRepository repository;

    public CompanionNotificationQueryAdapter(final CompanionNotificationQueryJpaRepository repository) {
        this.repository = repository;
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

