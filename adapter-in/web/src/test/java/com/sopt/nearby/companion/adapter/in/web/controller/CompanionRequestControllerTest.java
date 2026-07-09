// 동행 신청 상세 조회 컨트롤러의 요청 파싱과 응답 형식을 검증한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sopt.nearby.companion.application.AcceptCompanionRequestCommand;
import com.sopt.nearby.companion.application.AcceptedCompanionRequestResult;
import com.sopt.nearby.companion.application.CompanionRequestReviewResult;
import com.sopt.nearby.companion.application.ReadCompanionRequestReviewCommand;
import com.sopt.nearby.companion.application.RejectCompanionRequestCommand;
import com.sopt.nearby.companion.application.RejectedCompanionRequestResult;
import com.sopt.nearby.companion.domain.exception.CompanionRequestNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestNotPendingException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionRequestHostOnlyException;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.in.AcceptCompanionRequestUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionRequestReviewUseCase;
import com.sopt.nearby.companion.port.in.RejectCompanionRequestUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompanionRequestControllerTest {

    private MockMvc mockMvc;
    private FakeReadCompanionRequestReviewUseCase useCase;
    private FakeAcceptCompanionRequestUseCase acceptUseCase;
    private FakeRejectCompanionRequestUseCase rejectUseCase;

    @BeforeEach
    void setUp() {
        useCase = new FakeReadCompanionRequestReviewUseCase();
        acceptUseCase = new FakeAcceptCompanionRequestUseCase();
        rejectUseCase = new FakeRejectCompanionRequestUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanionRequestController(useCase, acceptUseCase, rejectUseCase))
                .setMessageConverters(jsonMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void readsCompanionRequestReviewAndPassesAuthenticatedHostIdToUseCase() throws Exception {
        useCase.result = reviewResult(LocalDateTime.of(2026, 7, 1, 9, 0));

        mockMvc.perform(get("/api/companion-requests/{applicationId}/review", 3L)
                        .principal(principal("100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_COMPANION_REQUEST_REVIEW"))
                .andExpect(jsonPath("$.message").value("동행 신청 상세 정보를 조회했어요."))
                .andExpect(jsonPath("$.data.applicationId").value(3))
                .andExpect(jsonPath("$.data.postId").value(10))
                .andExpect(jsonPath("$.data.applicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.placeName").value("오노테라"))
                .andExpect(jsonPath("$.data.meetingAt").value("2026-06-18T16:30:00"))
                .andExpect(jsonPath("$.data.applicantProfile.profileImageUrl")
                        .value("https://cdn.nearby/profile/2.png"))
                .andExpect(jsonPath("$.data.applicantProfile.nickname").value("지민"))
                .andExpect(jsonPath("$.data.applicantProfile.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.applicantProfile.birthYear").value(2003))
                .andExpect(jsonPath("$.data.applicantProfile.mannerScore").value(4.0))
                .andExpect(jsonPath("$.data.applicantAccount.phoneVerifiedAt")
                        .value("2026-07-01T09:00:00"));

        assertEquals(100L, useCase.command.hostUserId());
        assertEquals(3L, useCase.command.applicationId());
    }

    @Test
    void returnsNullPhoneVerifiedAtWhenApplicantIsNotVerified() throws Exception {
        useCase.result = reviewResult(null);

        mockMvc.perform(get("/api/companion-requests/{applicationId}/review", 3L)
                        .principal(principal("100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicantAccount.phoneVerifiedAt").value(nullValue()));
    }

    @Test
    void returnsForbiddenWhenCurrentUserIsNotHost() throws Exception {
        useCase.exception = new ForbiddenCompanionRequestHostOnlyException();

        mockMvc.perform(get("/api/companion-requests/{applicationId}/review", 3L)
                        .principal(principal("7")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN_COMPANION_REQUEST_HOST_ONLY"))
                .andExpect(jsonPath("$.message").value("해당 동행의 호스트만 처리할 수 있습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsNotFoundWhenCompanionRequestIsMissing() throws Exception {
        useCase.exception = new CompanionRequestNotFoundException();

        mockMvc.perform(get("/api/companion-requests/{applicationId}/review", 99L)
                        .principal(principal("100")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMPANION_REQUEST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("동행 신청을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void acceptsCompanionRequestAndPassesAuthenticatedHostIdToUseCase() throws Exception {
        acceptUseCase.result = new AcceptedCompanionRequestResult(
                3L,
                10L,
                CompanionApplicationStatus.ACCEPTED,
                1L,
                CompanionMatchStatus.MATCHED,
                LocalDateTime.of(2026, 7, 15, 12, 10)
        );

        mockMvc.perform(patch("/api/companion-requests/{applicationId}/accept", 3L)
                        .principal(principal("100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("ACCEPT_COMPANION_REQUEST"))
                .andExpect(jsonPath("$.message").value("동행 신청을 수락했어요."))
                .andExpect(jsonPath("$.data.applicationId").value(3))
                .andExpect(jsonPath("$.data.postId").value(10))
                .andExpect(jsonPath("$.data.applicationStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.matchId").value(1))
                .andExpect(jsonPath("$.data.matchStatus").value("MATCHED"))
                .andExpect(jsonPath("$.data.meetingAt").value("2026-07-15T12:10:00"));

        assertEquals(100L, acceptUseCase.command.hostUserId());
        assertEquals(3L, acceptUseCase.command.applicationId());
    }

    @Test
    void rejectsCompanionRequestAndPassesReasonToUseCase() throws Exception {
        rejectUseCase.result = new RejectedCompanionRequestResult(
                3L,
                10L,
                CompanionApplicationStatus.REJECTED
        );

        mockMvc.perform(patch("/api/companion-requests/{applicationId}/reject", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rejectionReason": "일정이 맞지 않아요"
                                }
                                """)
                        .principal(principal("100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("REJECT_COMPANION_REQUEST"))
                .andExpect(jsonPath("$.message").value("동행 신청을 거절했어요."))
                .andExpect(jsonPath("$.data.applicationId").value(3))
                .andExpect(jsonPath("$.data.postId").value(10))
                .andExpect(jsonPath("$.data.applicationStatus").value("REJECTED"));

        assertEquals(100L, rejectUseCase.command.hostUserId());
        assertEquals(3L, rejectUseCase.command.applicationId());
        assertEquals("일정이 맞지 않아요", rejectUseCase.command.rejectionReason());
    }

    @Test
    void rejectsCompanionRequestWithoutBody() throws Exception {
        rejectUseCase.result = new RejectedCompanionRequestResult(
                3L,
                10L,
                CompanionApplicationStatus.REJECTED
        );

        mockMvc.perform(patch("/api/companion-requests/{applicationId}/reject", 3L)
                        .principal(principal("100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("REJECT_COMPANION_REQUEST"));

        assertEquals(100L, rejectUseCase.command.hostUserId());
        assertEquals(3L, rejectUseCase.command.applicationId());
        assertEquals(null, rejectUseCase.command.rejectionReason());
    }

    @Test
    void returnsConflictWhenCompanionRequestIsNotPending() throws Exception {
        acceptUseCase.exception = new CompanionRequestNotPendingException();

        mockMvc.perform(patch("/api/companion-requests/{applicationId}/accept", 3L)
                        .principal(principal("100")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMPANION_REQUEST_NOT_PENDING"))
                .andExpect(jsonPath("$.message").value("대기 중인 신청만 처리할 수 있습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private CompanionRequestReviewResult reviewResult(final LocalDateTime phoneVerifiedAt) {
        return new CompanionRequestReviewResult(
                3L,
                10L,
                CompanionApplicationStatus.PENDING,
                "오노테라",
                LocalDateTime.of(2026, 6, 18, 16, 30),
                new CompanionRequestReviewResult.ApplicantProfile(
                        "https://cdn.nearby/profile/2.png",
                        "지민",
                        UserGender.FEMALE,
                        2003,
                        new BigDecimal("4.00")
                ),
                new CompanionRequestReviewResult.ApplicantAccount(phoneVerifiedAt)
        );
    }

    private Principal principal(final String name) {
        return () -> name;
    }

    private MappingJackson2HttpMessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    private static final class FakeReadCompanionRequestReviewUseCase
            implements ReadCompanionRequestReviewUseCase {

        private CompanionRequestReviewResult result;
        private ReadCompanionRequestReviewCommand command;
        private RuntimeException exception;

        @Override
        public CompanionRequestReviewResult read(final ReadCompanionRequestReviewCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }

    private static final class FakeAcceptCompanionRequestUseCase implements AcceptCompanionRequestUseCase {

        private AcceptedCompanionRequestResult result;
        private AcceptCompanionRequestCommand command;
        private RuntimeException exception;

        @Override
        public AcceptedCompanionRequestResult accept(final AcceptCompanionRequestCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }

    private static final class FakeRejectCompanionRequestUseCase implements RejectCompanionRequestUseCase {

        private RejectedCompanionRequestResult result;
        private RejectCompanionRequestCommand command;
        private RuntimeException exception;

        @Override
        public RejectedCompanionRequestResult reject(final RejectCompanionRequestCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
