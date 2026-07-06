// 동행 알림 목록 조회용 Query Port를 정의하는 인터페이스
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import java.util.List;

public interface CompanionNotificationQueryPort {

    List<CompanionNotificationSummary> findAllByUserIdAndDirection(
            Long userId,
            CompanionNotificationDirection direction
    );
}

