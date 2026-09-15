// 외부 푸시 공급자의 기기별 처리 결과를 표현한다.
package com.sopt.nearby.companion.port.out;

public record PushDeliveryResult(
        Long deliveryId,
        Outcome outcome,
        String providerMessageId,
        String errorCode,
        Long retryAfterSeconds
) {

    public enum Outcome {
        SENT,
        RETRYABLE_FAILURE,
        PERMANENT_FAILURE
    }
}
