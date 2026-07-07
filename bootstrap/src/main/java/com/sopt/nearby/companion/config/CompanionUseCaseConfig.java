// 동행 유스케이스 구현체를 Spring Bean으로 조립하는 설정 클래스
package com.sopt.nearby.companion.config;

import com.sopt.nearby.companion.application.CreateCompanionPostService;
import com.sopt.nearby.companion.application.CheckInCompanionMeetingService;
import com.sopt.nearby.companion.application.ConfirmCompanionScheduleService;
import com.sopt.nearby.companion.application.CreateCompanionNotificationService;
import com.sopt.nearby.companion.application.IssueProfileImageUploadUrlService;
import com.sopt.nearby.companion.application.MarkCompanionNotificationAsReadService;
import com.sopt.nearby.companion.application.ReadCompanionNotificationsService;
import com.sopt.nearby.companion.application.ReadCompanionPostDetailService;
import com.sopt.nearby.companion.application.ReadCompanionProfileService;
import com.sopt.nearby.companion.application.ReadCompanionPostsService;
import com.sopt.nearby.companion.application.ReadCompanionMatchesService;
import com.sopt.nearby.companion.application.ReadCompanionScheduleService;
import com.sopt.nearby.companion.application.RegisterCompanionProfileService;
import com.sopt.nearby.companion.port.in.CreateCompanionPostUseCase;
import com.sopt.nearby.companion.port.in.CheckInCompanionMeetingUseCase;
import com.sopt.nearby.companion.port.in.ConfirmCompanionScheduleUseCase;
import com.sopt.nearby.companion.port.in.CreateCompanionNotificationUseCase;
import com.sopt.nearby.companion.port.in.IssueProfileImageUploadUrlUseCase;
import com.sopt.nearby.companion.port.in.MarkCompanionNotificationAsReadUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionNotificationsUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionPostDetailUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionProfileUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionPostsUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchPreviewUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchesUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionScheduleUseCase;
import com.sopt.nearby.companion.port.in.RegisterCompanionProfileUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionMeetingCheckInQueryPort;
import com.sopt.nearby.companion.port.out.CompanionNotificationRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchSummaryQueryPort;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionPostDetailQueryPort;
import com.sopt.nearby.companion.port.out.CompanionPostStyleRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileDetailQueryPort;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileStyleRepository;
import com.sopt.nearby.companion.port.out.CompanionNotificationQueryPort;
import com.sopt.nearby.companion.port.out.CompanionPostQueryPort;
import com.sopt.nearby.companion.port.out.ProfileImageUploadUrlIssuer;
import com.sopt.nearby.companion.application.ReadCompanionMatchPreviewService;
import com.sopt.nearby.companion.port.out.CompanionScheduleDetailQueryPort;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheUseCase;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.user.port.in.CompleteCompanionProfileOnboardingUseCase;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompanionUseCaseConfig {

    @Bean
    CreateCompanionPostUseCase createCompanionPostUseCase(
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase,
            final ResolvePlaceCacheUseCase resolvePlaceCacheUseCase,
            final CompanionPostRepository companionPostRepository,
            final CompanionPostStyleRepository companionPostStyleRepository
    ) {
        return new CreateCompanionPostService(
                requireCompletedOnboardingUseCase,
                resolvePlaceCacheUseCase,
                companionPostRepository,
                companionPostStyleRepository,
                Clock.systemDefaultZone()
        );
    }

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
    ReadCompanionScheduleUseCase readCompanionScheduleUseCase(
            final CompanionScheduleDetailQueryPort queryPort,
            final CompanionMatchParticipantRepository companionMatchParticipantRepository
    ) {
        return new ReadCompanionScheduleService(queryPort, companionMatchParticipantRepository);
    }

    @Bean
    ReadCompanionPostsUseCase readCompanionPostsUseCase(
            final CompanionPostQueryPort queryPort,
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase,
            final ResolvePlaceImageUseCase resolvePlaceImageUseCase
    ) {
        return new ReadCompanionPostsService(
                queryPort,
                requireCompletedOnboardingUseCase,
                Clock.systemDefaultZone(),
                resolvePlaceImageUseCase
        );
    }

    @Bean
    ReadCompanionPostDetailUseCase readCompanionPostDetailUseCase(
            final CompanionPostDetailQueryPort queryPort,
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase
    ) {
        return new ReadCompanionPostDetailService(
                queryPort,
                requireCompletedOnboardingUseCase,
                Clock.systemDefaultZone()
        );
    }

    @Bean
    ReadCompanionProfileUseCase readCompanionProfileUseCase(
            final CompanionProfileDetailQueryPort queryPort,
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase
    ) {
        return new ReadCompanionProfileService(queryPort, requireCompletedOnboardingUseCase);
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

    @Bean
    IssueProfileImageUploadUrlUseCase issueProfileImageUploadUrlUseCase(
            final ProfileImageUploadUrlIssuer issuer,
            @Value("${nearby.storage.profile-image.max-file-size-bytes:5242880}") final long maxFileSizeBytes
    ) {
        return new IssueProfileImageUploadUrlService(issuer, maxFileSizeBytes);
    }

    @Bean
    RegisterCompanionProfileUseCase registerCompanionProfileUseCase(
            final CompanionProfileRepository companionProfileRepository,
            final CompanionProfileStyleRepository companionProfileStyleRepository,
            final CompleteCompanionProfileOnboardingUseCase completeOnboardingUseCase
    ) {
        return new RegisterCompanionProfileService(
                companionProfileRepository,
                companionProfileStyleRepository,
                completeOnboardingUseCase
        );
    }

    @Bean
    ReadCompanionNotificationsUseCase readCompanionNotificationsUseCase(
            final CompanionNotificationQueryPort queryPort
    ) {
        return new ReadCompanionNotificationsService(queryPort);
    }

    @Bean
    CreateCompanionNotificationUseCase createCompanionNotificationUseCase(
            final CompanionNotificationRepository repository
    ) {
        return new CreateCompanionNotificationService(repository, Clock.systemDefaultZone());
    }

    @Bean
    MarkCompanionNotificationAsReadUseCase markCompanionNotificationAsReadUseCase(
            final CompanionNotificationRepository repository
    ) {
        return new MarkCompanionNotificationAsReadService(repository, Clock.systemDefaultZone());
    }

    @Bean
    CheckInCompanionMeetingUseCase checkInCompanionMeetingUseCase(
            final CompanionMeetingCheckInQueryPort queryPort,
            final CompanionMatchParticipantRepository participantRepository,
            final MeetingCheckInRepository checkInRepository
    ) {
        return new CheckInCompanionMeetingService(
                queryPort,
                participantRepository,
                checkInRepository,
                Clock.systemDefaultZone()
        );
    }
}
