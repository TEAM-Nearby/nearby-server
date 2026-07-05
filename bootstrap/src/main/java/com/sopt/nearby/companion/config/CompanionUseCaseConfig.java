// 동행 유스케이스 구현체를 Spring Bean으로 조립하는 설정 클래스
package com.sopt.nearby.companion.config;

import com.sopt.nearby.companion.application.ConfirmCompanionScheduleService;
import com.sopt.nearby.companion.application.ReadCompanionMatchesService;
import com.sopt.nearby.companion.port.in.ConfirmCompanionScheduleUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchPreviewUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchesUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchSummaryQueryPort;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.companion.application.ReadCompanionMatchPreviewService;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheUseCase;
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

    @Bean
    ReadCompanionMatchesUseCase readCompanionMatchesUseCase(
            final CompanionMatchSummaryQueryPort queryPort
    ) {
        return new ReadCompanionMatchesService(queryPort);
    }

    @Bean
    ConfirmCompanionScheduleUseCase confirmCompanionScheduleUseCase(
            final CompanionMatchRepository companionMatchRepository,
            final CompanionPostRepository companionPostRepository,
            final CompanionScheduleRepository companionScheduleRepository,
            final ResolvePlaceCacheUseCase resolvePlaceCacheUseCase
    ) {
        return new ConfirmCompanionScheduleService(
                companionMatchRepository,
                companionPostRepository,
                companionScheduleRepository,
                resolvePlaceCacheUseCase
        );
    }
}
