// 동행 알림 목록 조회 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import com.sopt.nearby.companion.port.in.ReadCompanionNotificationsUseCase;
import com.sopt.nearby.companion.port.out.CompanionNotificationQueryPort;
import java.util.List;

public class ReadCompanionNotificationsService implements ReadCompanionNotificationsUseCase {

    private final CompanionNotificationQueryPort queryPort;

    public ReadCompanionNotificationsService(final CompanionNotificationQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public List<CompanionNotificationSummary> getNotifications(
            final Long userId,
            final CompanionNotificationDirection direction
    ) {
        return queryPort.findAllByUserIdAndDirection(userId, direction);
    }
}

