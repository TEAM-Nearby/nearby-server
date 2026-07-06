// 동행 알림 목록 조회 UseCase를 정의하는 인터페이스
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import java.util.List;

public interface ReadCompanionNotificationsUseCase {

    List<CompanionNotificationSummary> getNotifications(Long userId, CompanionNotificationDirection direction);
}

