// 동행 알림 자동 생성 UseCase를 정의하는 인터페이스
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.CreateCompanionNotificationCommand;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;

public interface CreateCompanionNotificationUseCase {

    CompanionNotification create(CreateCompanionNotificationCommand command);
}
