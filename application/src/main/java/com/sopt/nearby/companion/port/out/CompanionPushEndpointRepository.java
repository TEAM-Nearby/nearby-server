// 동행 푸시 수신 대상 저장소 포트를 정의한다.
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.notification.CompanionPushEndpoint;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface CompanionPushEndpointRepository {

    Optional<CompanionPushEndpoint> findByUserIdAndInstallationId(Long userId, String installationId);

    List<CompanionPushEndpoint> findActiveByUserId(Long userId);

    int deactivateByTokenExceptUser(String token, Long userId, LocalDateTime now);

    int deactivateById(Long endpointId, LocalDateTime now);

    default int deactivateIfCurrent(
            final Long endpointId,
            final long registrationVersion,
            final String token,
            final LocalDateTime now
    ) {
        return deactivateById(endpointId, now);
    }

    CompanionPushEndpoint save(CompanionPushEndpoint endpoint);
}
