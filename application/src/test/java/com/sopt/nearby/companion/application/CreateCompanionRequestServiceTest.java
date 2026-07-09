// 동행 신청 생성 서비스의 저장, 알림 생성, 모집 상태 검증을 테스트한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompanionPostNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionPostNotRecruitingException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestAlreadyExistsException;
import com.sopt.nearby.companion.domain.model.match.CompanionApplication;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.port.in.CreateCompanionNotificationUseCase;
import com.sopt.nearby.companion.port.out.CompanionApplicationRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateCompanionRequestServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-15T12:30:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 12, 30);

    private FakeRequireCompletedOnboardingUseCase onboardingUseCase;
    private FakeCompanionPostRepository postRepository;
    private FakeCompanionApplicationRepository applicationRepository;
    private FakeCreateCompanionNotificationUseCase notificationUseCase;
    private CreateCompanionRequestService service;

    @BeforeEach
    void setUp() {
        onboardingUseCase = new FakeRequireCompletedOnboardingUseCase();
        postRepository = new FakeCompanionPostRepository();
        applicationRepository = new FakeCompanionApplicationRepository();
        notificationUseCase = new FakeCreateCompanionNotificationUseCase();
        service = new CreateCompanionRequestService(
                onboardingUseCase,
                postRepository,
                applicationRepository,
                notificationUseCase,
                CLOCK
        );
    }

    @Test
    void createsPendingRequestAndNotificationForRecruitingPost() {
        postRepository.post = Optional.of(post(
                CompanionPostStatus.RECRUITING,
                CompanionPostMeetingTimeType.SCHEDULED,
                NOW.plusHours(2),
                null
        ));

        CreateCompanionRequestResult result = service.create(new CreateCompanionRequestCommand(7L, 10L));

        assertEquals(7L, onboardingUseCase.userId);
        assertEquals(10L, postRepository.postId);
        assertEquals(10L, applicationRepository.existsPostId);
        assertEquals(7L, applicationRepository.existsApplicantUserId);
        assertEquals(1L, result.applicationId());
        assertEquals(10L, result.postId());
        assertEquals(CompanionApplicationStatus.PENDING, result.applicationStatus());
        assertEquals(NOW, result.createdAt());
        assertEquals(7L, applicationRepository.saved.applicantUserId());
        assertEquals(CompanionApplicationStatus.PENDING, applicationRepository.saved.status());
        assertNull(applicationRepository.saved.rejectionReason());
        assertEquals(100L, notificationUseCase.command.recipientUserId());
        assertEquals(CompanionNotificationType.COMPANION_APPLICATION_CREATED, notificationUseCase.command.notificationType());
        assertEquals(CompanionNotificationTargetType.COMPANION_APPLICATION, notificationUseCase.command.targetType());
        assertEquals(1L, notificationUseCase.command.targetId());
    }

    @Test
    void rejectsMissingPost() {
        postRepository.post = Optional.empty();

        assertThrows(
                CompanionPostNotFoundException.class,
                () -> service.create(new CreateCompanionRequestCommand(7L, 10L))
        );
    }

    @Test
    void rejectsExistingRequestHistory() {
        postRepository.post = Optional.of(post(
                CompanionPostStatus.RECRUITING,
                CompanionPostMeetingTimeType.UNDECIDED,
                null,
                null
        ));
        applicationRepository.exists = true;

        assertThrows(
                CompanionRequestAlreadyExistsException.class,
                () -> service.create(new CreateCompanionRequestCommand(7L, 10L))
        );
        assertNull(applicationRepository.saved);
        assertNull(notificationUseCase.command);
    }

    @Test
    void rejectsClosedOrCanceledPost() {
        for (CompanionPostStatus status : new CompanionPostStatus[]{
                CompanionPostStatus.CLOSED,
                CompanionPostStatus.CANCELED
        }) {
            postRepository.post = Optional.of(post(
                    status,
                    CompanionPostMeetingTimeType.UNDECIDED,
                    null,
                    null
            ));

            assertThrows(
                    CompanionPostNotRecruitingException.class,
                    () -> service.create(new CreateCompanionRequestCommand(7L, 10L))
            );
        }
    }

    @Test
    void rejectsExpiredNowPost() {
        postRepository.post = Optional.of(post(
                CompanionPostStatus.RECRUITING,
                CompanionPostMeetingTimeType.NOW,
                null,
                NOW
        ));

        assertThrows(
                CompanionPostNotRecruitingException.class,
                () -> service.create(new CreateCompanionRequestCommand(7L, 10L))
        );
    }

    @Test
    void rejectsPastScheduledPost() {
        postRepository.post = Optional.of(post(
                CompanionPostStatus.RECRUITING,
                CompanionPostMeetingTimeType.SCHEDULED,
                NOW,
                null
        ));

        assertThrows(
                CompanionPostNotRecruitingException.class,
                () -> service.create(new CreateCompanionRequestCommand(7L, 10L))
        );
    }

    private CompanionPost post(
            final CompanionPostStatus status,
            final CompanionPostMeetingTimeType meetingTimeType,
            final LocalDateTime meetingAt,
            final LocalDateTime exposureExpiresAt
    ) {
        return new CompanionPost(
                10L,
                100L,
                20L,
                meetingTimeType,
                meetingAt,
                exposureExpiresAt,
                4,
                true,
                "같이 밥 먹을 동행을 구해요.",
                "https://open.kakao.com/o/nearby123",
                status,
                NOW.minusHours(1)
        );
    }

    private static final class FakeRequireCompletedOnboardingUseCase implements RequireCompletedOnboardingUseCase {

        private Long userId;

        @Override
        public void requireCompleted(final Long userId) {
            this.userId = userId;
        }
    }

    private static final class FakeCompanionPostRepository implements CompanionPostRepository {

        private Optional<CompanionPost> post = Optional.empty();
        private Long postId;

        @Override
        public CompanionPost save(final CompanionPost model) {
            throw new UnsupportedOperationException("동행 신청 생성 테스트에서는 모집글 저장을 사용하지 않습니다.");
        }

        @Override
        public Optional<CompanionPost> findById(final Long id) {
            postId = id;
            return post;
        }
    }

    private static final class FakeCompanionApplicationRepository implements CompanionApplicationRepository {

        private boolean exists;
        private Long existsPostId;
        private Long existsApplicantUserId;
        private CompanionApplication saved;

        @Override
        public CompanionApplication save(final CompanionApplication model) {
            saved = new CompanionApplication(
                    1L,
                    model.postId(),
                    model.applicantUserId(),
                    model.status(),
                    model.rejectionReason(),
                    model.createdAt()
            );
            return saved;
        }

        @Override
        public Optional<CompanionApplication> findById(final Long id) {
            return Optional.empty();
        }

        @Override
        public boolean existsByPostIdAndApplicantUserId(final Long postId, final Long applicantUserId) {
            existsPostId = postId;
            existsApplicantUserId = applicantUserId;
            return exists;
        }

        @Override
        public boolean updateStatusIfPending(
                final Long applicationId,
                final CompanionApplicationStatus status,
                final String rejectionReason
        ) {
            return false;
        }
    }

    private static final class FakeCreateCompanionNotificationUseCase implements CreateCompanionNotificationUseCase {

        private CreateCompanionNotificationCommand command;

        @Override
        public CompanionNotification create(final CreateCompanionNotificationCommand command) {
            this.command = command;
            return new CompanionNotification(
                    1L,
                    command.recipientUserId(),
                    command.notificationType(),
                    command.targetType(),
                    command.targetId(),
                    null,
                    NOW
            );
        }
    }
}
