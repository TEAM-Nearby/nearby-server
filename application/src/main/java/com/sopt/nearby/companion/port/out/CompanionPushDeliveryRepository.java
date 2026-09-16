// 동행 푸시 발송 작업 저장소 포트를 정의한다.
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.notification.CompanionPushDelivery;
import java.time.LocalDateTime;
import java.util.List;

public interface CompanionPushDeliveryRepository {

    CompanionPushDelivery saveIfAbsent(CompanionPushDelivery delivery);

    List<CompanionPushDelivery> claimDue(
            int batchSize,
            LocalDateTime now,
            LocalDateTime leaseUntil,
            String claimToken
    );

    boolean markSent(Long deliveryId, String claimToken, String providerMessageId, LocalDateTime now);

    boolean markRetry(
            Long deliveryId,
            String claimToken,
            LocalDateTime nextAttemptAt,
            String errorCode,
            LocalDateTime now
    );

    boolean markPermanentFailure(Long deliveryId, String claimToken, String errorCode, LocalDateTime now);

    boolean markExpired(Long deliveryId, String claimToken, String errorCode, LocalDateTime now);

    int skipInactiveEndpointDeliveries(LocalDateTime now, int limit);

    int expireExpiredDeliveries(LocalDateTime now, int limit);

    int recoverExpiredLeases(LocalDateTime now, int maxAttempts, int limit);
}
