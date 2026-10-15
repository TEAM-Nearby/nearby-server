// 동행 알림 읽음 처리 UseCase를 정의하는 인터페이스
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.MarkCompanionNotificationAsReadResult;

public interface MarkCompanionNotificationAsReadUseCase {

    MarkCompanionNotificationAsReadResult markAsRead(Long userId, Long notificationId);
}