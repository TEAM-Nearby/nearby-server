// 동행 모집글 신청 생성 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionPostNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionPostNotRecruitingException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestAlreadyExistsException;
import com.sopt.nearby.companion.domain.model.match.CompanionApplication;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.port.in.CreateCompanionNotificationUseCase;
import com.sopt.nearby.companion.port.in.CreateCompanionRequestUseCase;
import com.sopt.nearby.companion.port.out.CompanionApplicationRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

public class CreateCompanionRequestService implements CreateCompanionRequestUseCase {

    private final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase;
    private final CompanionPostRepository postRepository;
    private final CompanionApplicationRepository applicationRepository;
    private final CreateCompanionNotificationUseCase createCompanionNotificationUseCase;
    private final Clock clock;

    public CreateCompanionRequestService(
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase,
            final CompanionPostRepository postRepository,
            final CompanionApplicationRepository applicationRepository,
            final CreateCompanionNotificationUseCase createCompanionNotificationUseCase,
            final Clock clock
    ) {
        this.requireCompletedOnboardingUseCase = requireCompletedOnboardingUseCase;
        this.postRepository = postRepository;
        this.applicationRepository = applicationRepository;
        this.createCompanionNotificationUseCase = createCompanionNotificationUseCase;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreateCompanionRequestResult create(final CreateCompanionRequestCommand command) {
        validate(command);
        requireCompletedOnboardingUseCase.requireCompleted(command.applicantUserId());

        CompanionPost post = postRepository.findById(command.postId())
                .orElseThrow(CompanionPostNotFoundException::new);
        validateRecruiting(post);
        if (applicationRepository.existsByPostIdAndApplicantUserId(post.id(), command.applicantUserId())) {
            throw new CompanionRequestAlreadyExistsException();
        }

        LocalDateTime createdAt = LocalDateTime.now(clock);
        CompanionApplication application = applicationRepository.save(new CompanionApplication(
                null,
                post.id(),
                command.applicantUserId(),
                CompanionApplicationStatus.PENDING,
                null,
                createdAt
        ));
        createCompanionNotificationUseCase.create(new CreateCompanionNotificationCommand(
                post.hostUserId(),
                CompanionNotificationType.COMPANION_APPLICATION_CREATED,
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                application.id()
        ));

        return new CreateCompanionRequestResult(
                application.id(),
                application.postId(),
                application.status(),
                application.createdAt()
        );
    }

    private void validate(final CreateCompanionRequestCommand command) {
        if (command == null
                || command.applicantUserId() == null
                || command.applicantUserId() <= 0
                || command.postId() == null
                || command.postId() <= 0) {
            throw new CompanionPostNotFoundException();
        }
    }

    private void validateRecruiting(final CompanionPost post) {
        if (post.status() != CompanionPostStatus.RECRUITING || isExpired(post)) {
            throw new CompanionPostNotRecruitingException();
        }
    }

    private boolean isExpired(final CompanionPost post) {
        LocalDateTime now = LocalDateTime.now(clock);
        return switch (post.meetingTimeType()) {
            case NOW -> post.exposureExpiresAt() == null || !post.exposureExpiresAt().isAfter(now);
            case SCHEDULED -> post.meetingAt() == null || !post.meetingAt().isAfter(now);
            case UNDECIDED -> false;
        };
    }
}
