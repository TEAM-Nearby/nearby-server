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

    public CompanionPushRuntimeValidator(
            final Environment environment,
            @Value("${nearby.push.worker.enabled:false}") final boolean workerEnabled,
            @Value("${nearby.push.firebase.project-id:}") final String firebaseProjectId
    ) {
        this.environment = environment;
        this.workerEnabled = workerEnabled;
        this.firebaseProjectId = firebaseProjectId;
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
    }
}
