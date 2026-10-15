// 동행 푸시 발송 워커를 주기적으로 실행하는 스케줄러다.
package com.sopt.nearby.companion.config;

import com.sopt.nearby.companion.port.in.ProcessCompanionPushDeliveriesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        prefix = "nearby.push.worker",
        name = "enabled",
        havingValue = "true"
)
public class CompanionPushDeliveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(CompanionPushDeliveryScheduler.class);

    private final ProcessCompanionPushDeliveriesUseCase useCase;

    public CompanionPushDeliveryScheduler(final ProcessCompanionPushDeliveriesUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(
            fixedDelayString = "${nearby.push.worker.fixed-delay-ms:1000}",
            initialDelayString = "${nearby.push.worker.initial-delay-ms:1000}"
    )
    public void process() {
        try {
            useCase.processBatch();
        } catch (RuntimeException exception) {
            log.error("동행 푸시 발송 배치 처리에 실패했습니다.", exception);
        }
    }
}
