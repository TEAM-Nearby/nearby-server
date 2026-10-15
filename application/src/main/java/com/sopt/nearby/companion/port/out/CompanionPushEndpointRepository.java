// 동행 푸시 수신 대상 저장소 포트를 정의한다.
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.notification.CompanionPushEndpoint;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushPlatform;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompanionPushEndpointRepository {

    Optional<CompanionPushEndpoint> findByUserIdAndInstallationId(Long userId, String installationId);

    List<CompanionPushEndpoint> findActiveByUserId(Long userId);

    int deactivateByTokenExceptUser(String token, Long userId, LocalDateTime now);

    int deactivateById(Long endpointId, LocalDateTime now);

    // 설치 식별자 기준 등록·갱신을 하나의 저장소 연산으로 처리한다.
    CompanionPushEndpoint upsert(
            Long userId,
            String installationId,
            String token,
            CompanionPushPlatform platform,
            LocalDateTime now
    );

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
