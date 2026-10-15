// 동행 푸시 발송 작업의 처리 상태를 표현한다.
package com.sopt.nearby.companion.domain.model.notification;

public enum CompanionPushDeliveryStatus {
    PENDING,
    PROCESSING,
    RETRY,
    SENT,
    FAILED_PERMANENT,
    EXPIRED,
    SKIPPED
}
