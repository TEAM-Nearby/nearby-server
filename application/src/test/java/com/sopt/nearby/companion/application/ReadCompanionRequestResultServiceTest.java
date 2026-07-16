// 동행 신청 결과 조회 서비스의 상태별 응답과 접근 규칙을 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestResultNotReadableException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestResultNotReadyException;
import com.sopt.nearby.companion.domain.model.match.AcceptedCompanionRequestDetail;
import com.sopt.nearby.companion.domain.model.match.CompanionApplication;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.port.out.AcceptedCompanionRequestDetailQueryPort;
import com.sopt.nearby.companion.port.out.CompanionApplicationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadCompanionRequestResultServiceTest {

    private static final Long REQUESTER_USER_ID = 7L;
    private static final Long APPLICATION_ID = 3L;

    private FakeCompanionApplicationRepository applicationRepository;
    private FakeAcceptedCompanionRequestDetailQueryPort detailQueryPort;
    private ReadCompanionRequestResultService service;

    @BeforeEach
    void setUp() {
        applicationRepository = new FakeCompanionApplicationRepository();
        detailQueryPort = new FakeAcceptedCompanionRequestDetailQueryPort();
        service = new ReadCompanionRequestResultService(applicationRepository, detailQueryPort);
    }

    @Test
    void returnsAcceptedDetailForMatchedApplication() {
        applicationRepository.save(application(CompanionApplicationStatus.ACCEPTED, null));
        detailQueryPort.result = Optional.of(detail(CompanionMatchStatus.MATCHED));

        CompanionRequestResult result = service.read(
                new ReadCompanionRequestResultCommand(REQUESTER_USER_ID, APPLICATION_ID)
        );

        assertEquals(APPLICATION_ID, result.applicationId());
        assertEquals(CompanionApplicationStatus.ACCEPTED, result.applicationStatus());
        assertEquals(21L, result.acceptedDetail().matchId());
        assertEquals(CompanionMatchStatus.MATCHED, result.acceptedDetail().matchStatus());
        assertEquals(10L, result.acceptedDetail().postId());
        assertEquals(100L, result.acceptedDetail().host().userId());
        assertEquals("호스트", result.acceptedDetail().host().nickname());
        assertEquals("https://cdn.nearby/host.png", result.acceptedDetail().host().profileImageUrl());
        assertEquals("place-1", result.acceptedDetail().place().googlePlaceId());
        assertEquals("오노테라", result.acceptedDetail().place().name());
        assertEquals("도쿄도 시부야구", result.acceptedDetail().place().address());
        assertEquals(new BigDecimal("35.6595"), result.acceptedDetail().place().latitude());
        assertEquals(new BigDecimal("139.7005"), result.acceptedDetail().place().longitude());
        assertEquals(CompanionPostMeetingTimeType.SCHEDULED, result.acceptedDetail().meetingTimeType());
        assertEquals(LocalDateTime.of(2026, 7, 20, 18, 0), result.acceptedDetail().meetingAt());
        assertEquals(2, result.acceptedDetail().participantCount());
        assertEquals(4, result.acceptedDetail().maxParticipants());
        assertEquals("https://open.kakao.com/o/nearby", result.acceptedDetail().openChatUrl());
        assertEquals(APPLICATION_ID, detailQueryPort.applicationId);
        assertEquals(REQUESTER_USER_ID, detailQueryPort.requesterUserId);
    }

    @Test
    void returnsAcceptedDetailForScheduleConfirmedApplication() {
        applicationRepository.save(application(CompanionApplicationStatus.ACCEPTED, null));
        detailQueryPort.result = Optional.of(detail(CompanionMatchStatus.SCHEDULE_CONFIRMED));

        CompanionRequestResult result = service.read(
                new ReadCompanionRequestResultCommand(REQUESTER_USER_ID, APPLICATION_ID)
        );

        assertEquals(CompanionMatchStatus.SCHEDULE_CONFIRMED, result.acceptedDetail().matchStatus());
    }

    @Test
    void returnsRejectedWithoutRejectionReasonOrAcceptedDetail() {
        applicationRepository.save(application(CompanionApplicationStatus.REJECTED, "개인 일정이 생겼어요"));

        CompanionRequestResult result = service.read(
                new ReadCompanionRequestResultCommand(REQUESTER_USER_ID, APPLICATION_ID)
        );

        assertEquals(APPLICATION_ID, result.applicationId());
        assertEquals(CompanionApplicationStatus.REJECTED, result.applicationStatus());
        assertNull(result.acceptedDetail());
        assertNull(detailQueryPort.applicationId);
    }

    @Test
    void rejectsPendingApplicationAsNotReady() {
        applicationRepository.save(application(CompanionApplicationStatus.PENDING, null));

        assertThrows(
                CompanionRequestResultNotReadyException.class,
                () -> service.read(new ReadCompanionRequestResultCommand(REQUESTER_USER_ID, APPLICATION_ID))
        );
    }

    @Test
    void rejectsCanceledApplicationAsNotReady() {
        applicationRepository.save(application(CompanionApplicationStatus.CANCELED, null));

        assertThrows(
                CompanionRequestResultNotReadyException.class,
                () -> service.read(new ReadCompanionRequestResultCommand(REQUESTER_USER_ID, APPLICATION_ID))
        );
    }

    @Test
    void hidesApplicationFromAnotherApplicant() {
        applicationRepository.save(new CompanionApplication(
                APPLICATION_ID,
                10L,
                8L,
                CompanionApplicationStatus.ACCEPTED,
                null,
                LocalDateTime.of(2026, 7, 13, 12, 0)
        ));

        assertThrows(
                CompanionRequestNotFoundException.class,
                () -> service.read(new ReadCompanionRequestResultCommand(REQUESTER_USER_ID, APPLICATION_ID))
        );
    }

    @Test
    void rejectsInvalidIdentifiersBeforeLoadingApplication() {
        assertThrows(
                CompanionRequestNotFoundException.class,
                () -> service.read(new ReadCompanionRequestResultCommand(0L, APPLICATION_ID))
        );
        assertThrows(
                CompanionRequestNotFoundException.class,
                () -> service.read(new ReadCompanionRequestResultCommand(REQUESTER_USER_ID, null))
        );
        assertNull(applicationRepository.requestedId);
    }

    @Test
    void rejectsMissingApplication() {
        assertThrows(
                CompanionRequestNotFoundException.class,
                () -> service.read(new ReadCompanionRequestResultCommand(REQUESTER_USER_ID, APPLICATION_ID))
        );
    }

    @Test
    void rejectsCanceledAcceptedMatchAsNotReadable() {
        applicationRepository.save(application(CompanionApplicationStatus.ACCEPTED, null));
        detailQueryPort.result = Optional.of(detail(CompanionMatchStatus.CANCELED));

        assertThrows(
                CompanionRequestResultNotReadableException.class,
                () -> service.read(new ReadCompanionRequestResultCommand(REQUESTER_USER_ID, APPLICATION_ID))
        );
    }

    @Test
    void rejectsCompletedAcceptedMatchAsNotReadable() {
        applicationRepository.save(application(CompanionApplicationStatus.ACCEPTED, null));
        detailQueryPort.result = Optional.of(detail(CompanionMatchStatus.COMPLETED));

        assertThrows(
                CompanionRequestResultNotReadableException.class,
                () -> service.read(new ReadCompanionRequestResultCommand(REQUESTER_USER_ID, APPLICATION_ID))
        );
    }

    @Test
    void rejectsAcceptedApplicationWithoutDetail() {
        applicationRepository.save(application(CompanionApplicationStatus.ACCEPTED, null));

        assertThrows(
                CompanionMatchNotFoundException.class,
                () -> service.read(new ReadCompanionRequestResultCommand(REQUESTER_USER_ID, APPLICATION_ID))
        );
    }

    private CompanionApplication application(
            final CompanionApplicationStatus status,
            final String rejectionReason
    ) {
        return new CompanionApplication(
                APPLICATION_ID,
                10L,
                REQUESTER_USER_ID,
                status,
                rejectionReason,
                LocalDateTime.of(2026, 7, 13, 12, 0)
        );
    }

    private AcceptedCompanionRequestDetail detail(final CompanionMatchStatus status) {
        return new AcceptedCompanionRequestDetail(
                21L,
                status,
                10L,
                new AcceptedCompanionRequestDetail.Host(
                        100L,
                        "호스트",
                        "https://cdn.nearby/host.png"
                ),
                new AcceptedCompanionRequestDetail.Place(
                        "place-1",
                        "오노테라",
                        "도쿄도 시부야구",
                        new BigDecimal("35.6595"),
                        new BigDecimal("139.7005")
                ),
                CompanionPostMeetingTimeType.SCHEDULED,
                LocalDateTime.of(2026, 7, 20, 18, 0),
                2,
                4,
                "https://open.kakao.com/o/nearby"
        );
    }

    private static final class FakeCompanionApplicationRepository implements CompanionApplicationRepository {

        private final Map<Long, CompanionApplication> applications = new HashMap<>();
        private Long requestedId;

        @Override
        public CompanionApplication save(final CompanionApplication model) {
            applications.put(model.id(), model);
            return model;
        }

        @Override
        public Optional<CompanionApplication> findById(final Long id) {
            requestedId = id;
            return Optional.ofNullable(applications.get(id));
        }

        @Override
        public boolean existsByPostIdAndApplicantUserId(final Long postId, final Long applicantUserId) {
            return false;
        }

        @Override
        public long countAcceptedByPostId(final Long postId) {
            return 0;
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

    private static final class FakeAcceptedCompanionRequestDetailQueryPort
            implements AcceptedCompanionRequestDetailQueryPort {

        private Optional<AcceptedCompanionRequestDetail> result = Optional.empty();
        private Long applicationId;
        private Long requesterUserId;

        @Override
        public Optional<AcceptedCompanionRequestDetail> findByApplicationIdAndRequesterUserId(
                final Long applicationId,
                final Long requesterUserId
        ) {
            this.applicationId = applicationId;
            this.requesterUserId = requesterUserId;
            return result;
        }
    }
}
