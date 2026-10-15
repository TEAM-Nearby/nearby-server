// 신청자가 동행 신청 처리 결과를 조회하는 HTTP 응답과 명령 전달을 검증한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sopt.nearby.companion.application.CompanionRequestResult;
import com.sopt.nearby.companion.application.ReadCompanionRequestResultCommand;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestResultNotReadableException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestResultNotReadyException;
import com.sopt.nearby.companion.domain.model.match.AcceptedCompanionRequestDetail;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.port.in.ReadCompanionRequestResultUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompanionRequestResultControllerTest {

    private MockMvc mockMvc;
    private FakeReadCompanionRequestResultUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FakeReadCompanionRequestResultUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanionRequestResultController(useCase))
                .setMessageConverters(jsonMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void readsAcceptedCompanionRequestResultAndForwardsApplicantCommand() throws Exception {
        useCase.result = new CompanionRequestResult(
                3L,
                CompanionApplicationStatus.ACCEPTED,
                new AcceptedCompanionRequestDetail(
                        7L,
                        CompanionMatchStatus.MATCHED,
                        10L,
                        new AcceptedCompanionRequestDetail.Host(
                                100L,
                                "수빈",
                                "https://cdn.nearby/profile/100.png"
                        ),
                        new AcceptedCompanionRequestDetail.Place(
                                "ChIJ-place",
                                "오노테라",
                                "서울특별시 강남구",
                                new BigDecimal("37.5012"),
                                new BigDecimal("127.0396")
                        ),
                        CompanionPostMeetingTimeType.SCHEDULED,
                        LocalDateTime.of(2026, 7, 15, 12, 10),
                        2,
                        4,
                        "https://open.kakao.com/o/example"
                )
        );

        mockMvc.perform(get("/api/users/me/companion-requests/{applicationId}/result", 3L)
                        .principal(principal("200")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_COMPANION_REQUEST_RESULT"))
                .andExpect(jsonPath("$.message").value("동행 신청 결과를 조회했어요."))
                .andExpect(jsonPath("$.data.applicationId").value(3))
                .andExpect(jsonPath("$.data.applicationStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.acceptedResult.matchId").value(7))
                .andExpect(jsonPath("$.data.acceptedResult.matchStatus").value("MATCHED"))
                .andExpect(jsonPath("$.data.acceptedResult.postId").value(10))
                .andExpect(jsonPath("$.data.acceptedResult.host.userId").value(100))
                .andExpect(jsonPath("$.data.acceptedResult.host.nickname").value("수빈"))
                .andExpect(jsonPath("$.data.acceptedResult.host.profileImageUrl")
                        .value("https://cdn.nearby/profile/100.png"))
                .andExpect(jsonPath("$.data.acceptedResult.place.googlePlaceId").value("ChIJ-place"))
                .andExpect(jsonPath("$.data.acceptedResult.place.name").value("오노테라"))
                .andExpect(jsonPath("$.data.acceptedResult.place.address").value("서울특별시 강남구"))
                .andExpect(jsonPath("$.data.acceptedResult.place.latitude").value(37.5012))
                .andExpect(jsonPath("$.data.acceptedResult.place.longitude").value(127.0396))
                .andExpect(jsonPath("$.data.acceptedResult.meetingTimeType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.acceptedResult.meetingAt").value("2026-07-15T12:10:00"))
                .andExpect(jsonPath("$.data.acceptedResult.participantCount").value(2))
                .andExpect(jsonPath("$.data.acceptedResult.maxParticipants").value(4))
                .andExpect(jsonPath("$.data.acceptedResult.openChatUrl")
                        .value("https://open.kakao.com/o/example"));

        assertEquals(200L, useCase.command.requesterUserId());
        assertEquals(3L, useCase.command.applicationId());
    }

    @Test
    void returnsNullAcceptedResultForRejectedApplication() throws Exception {
        useCase.result = new CompanionRequestResult(3L, CompanionApplicationStatus.REJECTED, null);

        mockMvc.perform(get("/api/users/me/companion-requests/{applicationId}/result", 3L)
                        .principal(principal("200")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applicationId").value(3))
                .andExpect(jsonPath("$.data.applicationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.acceptedResult").value(nullValue()));
    }

    @Test
    void returnsNotFoundWhenCompanionRequestIsMissing() throws Exception {
        useCase.exception = new CompanionRequestNotFoundException();

        mockMvc.perform(get("/api/users/me/companion-requests/{applicationId}/result", 99L)
                        .principal(principal("200")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANION_REQUEST_NOT_FOUND"));
    }

    @Test
    void returnsConflictWhenCompanionRequestResultIsNotReady() throws Exception {
        useCase.exception = new CompanionRequestResultNotReadyException();

        mockMvc.perform(get("/api/users/me/companion-requests/{applicationId}/result", 3L)
                        .principal(principal("200")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANION_REQUEST_RESULT_NOT_READY"));
    }

    @Test
    void returnsConflictWhenCompanionRequestResultIsNotReadable() throws Exception {
        useCase.exception = new CompanionRequestResultNotReadableException();

        mockMvc.perform(get("/api/users/me/companion-requests/{applicationId}/result", 3L)
                        .principal(principal("200")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMPANION_REQUEST_RESULT_NOT_READABLE"));
    }

    @Test
    void returnsNotFoundWhenCompanionMatchIsMissing() throws Exception {
        useCase.exception = new CompanionMatchNotFoundException();

        mockMvc.perform(get("/api/users/me/companion-requests/{applicationId}/result", 3L)
                        .principal(principal("200")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPANION_MATCH_NOT_FOUND"));
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

    private static final class FakeReadCompanionRequestResultUseCase
            implements ReadCompanionRequestResultUseCase {

        private CompanionRequestResult result;
        private ReadCompanionRequestResultCommand command;
        private RuntimeException exception;

        @Override
        public CompanionRequestResult read(final ReadCompanionRequestResultCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
