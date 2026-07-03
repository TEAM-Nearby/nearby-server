// 동행 유스케이스 구현체를 Spring Bean으로 조립하는 설정 클래스
package com.sopt.nearby.companion.config;

import com.sopt.nearby.companion.port.in.ReadCompanionMatchPreviewUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.companion.service.ReadCompanionMatchPreviewService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompanionUseCaseConfig {

    @Bean
    ReadCompanionMatchPreviewUseCase readCompanionMatchPreviewUseCase(
            CompanionMatchRepository companionMatchRepository,
            CompanionPostRepository companionPostRepository,
            CompanionMatchParticipantRepository companionMatchParticipantRepository,
            CompanionProfileRepository companionProfileRepository
    ) {
        return new ReadCompanionMatchPreviewService(
                companionMatchRepository,
                companionPostRepository,
                companionMatchParticipantRepository,
                companionProfileRepository
        );
    }
}
