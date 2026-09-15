// 동행 푸시 워커가 운영 발송 어댑터로 실행되는지 시작 시 검증한다.
package com.sopt.nearby.companion.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CompanionPushRuntimeValidator {

    private final Environment environment;
    private final boolean workerEnabled;
    private final String firebaseProjectId;
    private final long leaseSeconds;
    private final long maxFirebaseTimeoutMs;
    private final long leaseSafetyMarginSeconds;

    public CompanionPushRuntimeValidator(
            final Environment environment,
            @Value("${nearby.push.worker.enabled:false}") final boolean workerEnabled,
            @Value("${nearby.push.firebase.project-id:}") final String firebaseProjectId,
            @Value("${nearby.push.worker.lease-seconds:30}") final long leaseSeconds,
            @Value("${nearby.push.firebase.connect-timeout-ms:5000}") final long connectTimeoutMs,
            @Value("${nearby.push.firebase.read-timeout-ms:10000}") final long readTimeoutMs,
            @Value("${nearby.push.firebase.write-timeout-ms:10000}") final long writeTimeoutMs,
            @Value("${nearby.push.worker.lease-safety-margin-seconds:5}") final long leaseSafetyMarginSeconds
    ) {
        this.environment = environment;
        this.workerEnabled = workerEnabled;
        this.firebaseProjectId = firebaseProjectId;
        this.leaseSeconds = leaseSeconds;
        this.maxFirebaseTimeoutMs = Math.max(connectTimeoutMs, Math.max(readTimeoutMs, writeTimeoutMs));
        this.leaseSafetyMarginSeconds = leaseSafetyMarginSeconds;
        validate();
    }

    private void validate() {
        if (!workerEnabled) {
            return;
        }
        boolean fcmProfile = Arrays.asList(environment.getActiveProfiles()).contains("fcm");
        if (!fcmProfile) {
            throw new IllegalStateException(
                    "푸시 워커를 활성화하려면 fcm 프로필이 필요합니다. 로컬 푸시 어댑터로 운영 발송할 수 없습니다."
            );
        }
        if (firebaseProjectId == null || firebaseProjectId.isBlank()) {
            throw new IllegalStateException("푸시 워커를 활성화하려면 Firebase 프로젝트 ID가 필요합니다.");
        }
        long requiredLeaseSeconds = (maxFirebaseTimeoutMs + 999L) / 1000L + Math.max(0L, leaseSafetyMarginSeconds);
        if (leaseSeconds <= requiredLeaseSeconds) {
            throw new IllegalStateException(
                    "푸시 워커의 lease 시간은 Firebase HTTP 타임아웃과 안전 여유보다 길어야 합니다."
            );
        }
    }
}
