// 동행 신청 상세 조회 서비스의 권한과 meetingAt 산출 규칙을 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompanionRequestNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionRequestHostOnlyException;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionRequestReview;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionRequestReviewQueryPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadCompanionRequestReviewServiceTest {

    private FakeCompanionRequestReviewQueryPort queryPort;
    private ReadCompanionRequestReviewService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeCompanionRequestReviewQueryPort();
        service = new ReadCompanionRequestReviewService(queryPort);
    }

    @Test
    void returnsReviewForHost() {
        queryPort.result = Optional.of(review(
                100L,
                CompanionPostMeetingTimeType.SCHEDULED,
                LocalDateTime.of(2026, 6, 18, 16, 30),
                null
        ));

        CompanionRequestReviewResult result = service.read(new ReadCompanionRequestReviewCommand(100L, 3L));

        assertEquals(3L, queryPort.applicationId);
        assertEquals(3L, result.applicationId());
        assertEquals(10L, result.postId());
        assertEquals(CompanionApplicationStatus.PENDING, result.applicationStatus());
        assertEquals("오노테라", result.placeName());
        assertEquals(CompanionPostMeetingTimeType.SCHEDULED, result.meetingTimeType());
        assertEquals(LocalDateTime.of(2026, 6, 18, 16, 30), result.meetingAt());
        assertEquals(20L, result.applicantProfile().profileId());
        assertEquals("지민", result.applicantProfile().nickname());
        assertEquals(UserGender.FEMALE, result.applicantProfile().gender());
        assertEquals(LocalDateTime.of(2026, 7, 1, 9, 0), result.applicantAccount().phoneVerifiedAt());
    }

    @Test
    void fallsBackToExposureExpiresAtWhenMeetingAtIsNull() {
        queryPort.result = Optional.of(review(
                100L,
                CompanionPostMeetingTimeType.NOW,
                null,
                LocalDateTime.of(2026, 6, 18, 17, 30)
        ));

        CompanionRequestReviewResult result = service.read(new ReadCompanionRequestReviewCommand(100L, 3L));

        assertEquals(LocalDateTime.of(2026, 6, 18, 17, 30), result.meetingAt());
    }

    @Test
    void allowsNullMeetingAtWhenNoFallbackExists() {
        queryPort.result = Optional.of(review(100L, CompanionPostMeetingTimeType.UNDECIDED, null, null));

        CompanionRequestReviewResult result = service.read(new ReadCompanionRequestReviewCommand(100L, 3L));

        assertNull(result.meetingAt());
    }

    @Test
    void rejectsInvalidApplicationIdBeforeQuery() {
        assertThrows(
                CompanionRequestNotFoundException.class,
                () -> service.read(new ReadCompanionRequestReviewCommand(100L, 0L))
        );
        assertNull(queryPort.applicationId);
    }

    @Test
    void rejectsMissingCompanionRequest() {
        queryPort.result = Optional.empty();

        assertThrows(
                CompanionRequestNotFoundException.class,
                () -> service.read(new ReadCompanionRequestReviewCommand(100L, 3L))
        );
    }

    @Test
    void rejectsNonHostUser() {
        queryPort.result = Optional.of(review(
                100L,
                CompanionPostMeetingTimeType.SCHEDULED,
                LocalDateTime.of(2026, 6, 18, 16, 30),
                null
        ));

        assertThrows(
                ForbiddenCompanionRequestHostOnlyException.class,
                () -> service.read(new ReadCompanionRequestReviewCommand(7L, 3L))
        );
    }

    private CompanionRequestReview review(
            final Long hostUserId,
            final CompanionPostMeetingTimeType meetingTimeType,
            final LocalDateTime meetingAt,
            final LocalDateTime exposureExpiresAt
    ) {
        return new CompanionRequestReview(
                3L,
                10L,
                CompanionApplicationStatus.PENDING,
                hostUserId,
                "오노테라",
                meetingTimeType,
                meetingAt,
                exposureExpiresAt,
                new CompanionRequestReview.ApplicantProfile(
                        20L,
                        "https://cdn.nearby/profile/2.png",
                        "지민",
                        UserGender.FEMALE,
                        2003,
                        new BigDecimal("4.00")
                ),
                new CompanionRequestReview.ApplicantAccount(LocalDateTime.of(2026, 7, 1, 9, 0))
        );
    }

    private static final class FakeCompanionRequestReviewQueryPort implements CompanionRequestReviewQueryPort {

        private Optional<CompanionRequestReview> result = Optional.empty();
        private Long applicationId;

        @Override
        public Optional<CompanionRequestReview> findByApplicationId(final Long applicationId) {
            this.applicationId = applicationId;
            return result;
        }
    }
}
