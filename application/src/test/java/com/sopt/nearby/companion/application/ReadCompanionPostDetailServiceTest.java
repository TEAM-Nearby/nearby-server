// 동행 모집 글 상세 조회 서비스의 시간 타입별 응답과 접근 규칙을 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompanionPostExpiredException;
import com.sopt.nearby.companion.domain.exception.CompanionPostNotFoundException;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostApplyStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostDetail;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.out.CompanionPostDetailQueryPort;
import com.sopt.nearby.user.exception.OnboardingRequiredException;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadCompanionPostDetailServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-02T05:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 2, 5, 0);

    private FakeCompanionPostDetailQueryPort queryPort;
    private FakeRequireCompletedOnboardingUseCase onboardingUseCase;
    private ReadCompanionPostDetailService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeCompanionPostDetailQueryPort();
        onboardingUseCase = new FakeRequireCompletedOnboardingUseCase();
        service = new ReadCompanionPostDetailService(queryPort, onboardingUseCase, CLOCK);
    }

    @Test
    void returnsScheduledPostWithMeetingAtAsDeadlineAndHiddenOpenChat() {
        queryPort.result = Optional.of(detail(
                CompanionPostMeetingTimeType.SCHEDULED,
                NOW.plusHours(2),
                null,
                null
        ));

        CompanionPostDetailResult result = service.read(new ReadCompanionPostDetailCommand(7L, 101L));

        assertEquals(7L, onboardingUseCase.userId);
        assertEquals(101L, queryPort.postId);
        assertEquals(7L, queryPort.userId);
        assertEquals(101L, result.postId());
        assertEquals("google-place-id", result.googlePlaceId());
        assertEquals(NOW.plusHours(2), result.meetingAt());
        assertNull(result.expiresAt());
        assertNull(result.openChatUrl());
        assertEquals(CompanionPostApplyStatus.NOT_APPLIED, result.applyStatus());
        assertEquals("니어바이 스시", result.place().name());
        assertEquals(List.of(TravelStyleKeyword.PLANNED, TravelStyleKeyword.FOODIE), result.hostProfileSummary().keywords());
    }

    @Test
    void returnsNowPostWithExpiresAtAndOpenChatForAcceptedApplicant() {
        queryPort.result = Optional.of(detail(
                CompanionPostMeetingTimeType.NOW,
                null,
                NOW.plusMinutes(30),
                CompanionApplicationStatus.ACCEPTED
        ));

        CompanionPostDetailResult result = service.read(new ReadCompanionPostDetailCommand(7L, 101L));

        assertNull(result.meetingAt());
        assertEquals(NOW.plusMinutes(30), result.expiresAt());
        assertEquals("https://open.kakao.com/o/nearby123", result.openChatUrl());
        assertEquals(CompanionPostApplyStatus.ACCEPTED, result.applyStatus());
    }

    @Test
    void returnsUndecidedPostWithoutMeetingAtAndExpiresAt() {
        queryPort.result = Optional.of(detail(
                CompanionPostMeetingTimeType.UNDECIDED,
                null,
                null,
                CompanionApplicationStatus.PENDING
        ));

        CompanionPostDetailResult result = service.read(new ReadCompanionPostDetailCommand(7L, 101L));

        assertNull(result.meetingAt());
        assertNull(result.expiresAt());
        assertEquals(CompanionPostApplyStatus.PENDING, result.applyStatus());
    }

    @Test
    void returnsOpenChatUrlToHost() {
        queryPort.result = Optional.of(detail(
                CompanionPostMeetingTimeType.SCHEDULED,
                NOW.plusHours(2),
                null,
                null
        ));

        CompanionPostDetailResult result = service.read(new ReadCompanionPostDetailCommand(1L, 101L));

        assertEquals("https://open.kakao.com/o/nearby123", result.openChatUrl());
    }

    @Test
    void rejectsExpiredNowPost() {
        queryPort.result = Optional.of(detail(
                CompanionPostMeetingTimeType.NOW,
                null,
                NOW,
                null
        ));

        assertThrows(
                CompanionPostExpiredException.class,
                () -> service.read(new ReadCompanionPostDetailCommand(7L, 101L))
        );
    }

    @Test
    void rejectsExpiredScheduledPost() {
        queryPort.result = Optional.of(detail(
                CompanionPostMeetingTimeType.SCHEDULED,
                NOW,
                null,
                null
        ));

        assertThrows(
                CompanionPostExpiredException.class,
                () -> service.read(new ReadCompanionPostDetailCommand(7L, 101L))
        );
    }

    @Test
    void rejectsMissingPost() {
        queryPort.result = Optional.empty();

        assertThrows(
                CompanionPostNotFoundException.class,
                () -> service.read(new ReadCompanionPostDetailCommand(7L, 101L))
        );
    }

    @Test
    void rejectsUserWithoutCompletedOnboarding() {
        onboardingUseCase.exception = new OnboardingRequiredException();

        assertThrows(
                OnboardingRequiredException.class,
                () -> service.read(new ReadCompanionPostDetailCommand(7L, 101L))
        );
    }

    private CompanionPostDetail detail(
            final CompanionPostMeetingTimeType meetingTimeType,
            final LocalDateTime meetingAt,
            final LocalDateTime expiresAt,
            final CompanionApplicationStatus applicationStatus
    ) {
        return new CompanionPostDetail(
                101L,
                1L,
                meetingAt,
                4,
                "같이 스시 먹으러 갈 사람 구해요.",
                "https://open.kakao.com/o/nearby123",
                CompanionPostStatus.RECRUITING,
                LocalDateTime.of(2026, 7, 2, 4, 0),
                meetingTimeType,
                expiresAt,
                2,
                applicationStatus,
                new CompanionPostDetail.Place(
                        "google-place-id",
                        "니어바이 스시",
                        "서울시 어딘가",
                        new BigDecimal("37.56710000"),
                        new BigDecimal("126.97920000"),
                        CompanionPostPlaceCategory.RESTAURANT
                ),
                new CompanionPostDetail.HostProfileSummary(
                        5L,
                        "니어바이",
                        UserGender.FEMALE,
                        2001,
                        null,
                        new BigDecimal("4.00"),
                        LocalDateTime.of(2026, 7, 1, 10, 0),
                        List.of(TravelStyleKeyword.PLANNED, TravelStyleKeyword.FOODIE)
                )
        );
    }

    private static final class FakeCompanionPostDetailQueryPort implements CompanionPostDetailQueryPort {

        private Optional<CompanionPostDetail> result = Optional.empty();
        private Long postId;
        private Long userId;

        @Override
        public Optional<CompanionPostDetail> findByPostId(final Long postId, final Long userId) {
            this.postId = postId;
            this.userId = userId;
            return result;
        }
    }

    private static final class FakeRequireCompletedOnboardingUseCase implements RequireCompletedOnboardingUseCase {

        private Long userId;
        private RuntimeException exception;

        @Override
        public void requireCompleted(final Long userId) {
            this.userId = userId;
            if (exception != null) {
                throw exception;
            }
        }
    }
}
