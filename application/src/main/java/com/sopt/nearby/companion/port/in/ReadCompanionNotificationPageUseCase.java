// 동행 알림 커서 페이지 조회 UseCase를 정의한다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.CompanionNotificationPage;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;

public interface ReadCompanionNotificationPageUseCase {

    CompanionNotificationPage getPage(
            Long userId,
            CompanionNotificationDirection direction,
            int size,
            String cursor
    );
}
