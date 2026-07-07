// 동행 만남 컨트롤러의 체크인 요청 파싱과 응답 형식을 검증하는 테스트
package com.sopt.nearby.companion.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sopt.nearby.companion.application.CheckInCompanionMeetingCommand;
import com.sopt.nearby.companion.application.CheckInCompanionMeetingResult;
import com.sopt.nearby.companion.domain.exception.InvalidCheckInRequestException;
import com.sopt.nearby.companion.domain.exception.OutOfCheckInRadiusException;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.port.in.CheckInCompanionMeetingUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import java.security.Principal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompanionMeetingControllerTest {

    private MockMvc mockMvc;
    private FakeCheckInCompanionMeetingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FakeCheckInCompanionMeetingUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanionMeetingController(useCase))
                .setMessageConverters(jsonMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void checksInMeetingAndPassesAuthenticatedUserIdToUseCase() throws Exception {
        useCase.result = result(false);

        mockMvc.perform(post("/api/companion-meetings/{meetingId}/check-in", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 41.390205,
                                  "longitude": 2.163548
                                }
                                """)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("CHECK_IN_COMPANION_MEETING"))
                .andExpect(jsonPath("$.message").value("만남 인증이 완료되었어요."))
                .andExpect(jsonPath("$.data.meetingId").value(1))
                .andExpect(jsonPath("$.data.meetingStatus").value("ONGOING"))
                .andExpect(jsonPath("$.data.currentUserCheckedIn").value(true))
                .andExpect(jsonPath("$.data.checkedInCount").value(2))
                .andExpect(jsonPath("$.data.totalParticipantCount").value(3))
                .andExpect(jsonPath("$.data.allParticipantsCheckedIn").value(false))
                .andExpect(jsonPath("$.data.canMoveToComplete").value(false))
                .andExpect(jsonPath("$.data.checkedInAt").value("2026-06-29T18:35:00"))
                .andExpect(jsonPath("$.data.distanceMeters").value(24.5))
                .andExpect(jsonPath("$.data.allowedRadiusMeters").value(150.0))
                .andExpect(jsonPath("$.data.checkInAvailableFrom").value("2026-06-29T17:30:00"))
                .andExpect(jsonPath("$.data.checkInAvailableUntil").value("2026-06-29T19:30:00"));

        assertEquals(7L, useCase.command.userId());
        assertEquals(1L, useCase.command.meetingId());
        assertEquals(0, useCase.command.latitude().compareTo(new java.math.BigDecimal("41.390205")));
        assertEquals(0, useCase.command.longitude().compareTo(new java.math.BigDecimal("2.163548")));
    }

    @Test
    void returnsAlreadyCompletedSuccessCodeWhenUserAlreadyCheckedIn() throws Exception {
        useCase.result = result(true);

        mockMvc.perform(post("/api/companion-meetings/{meetingId}/check-in", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 41.390205,
                                  "longitude": 2.163548
                                }
                                """)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CHECK_IN_COMPANION_MEETING_ALREADY_COMPLETED"))
                .andExpect(jsonPath("$.message").value("이미 만남 인증이 완료되어 있어요."));
    }

    @Test
    void returnsOutOfRadiusError() throws Exception {
        useCase.exception = new OutOfCheckInRadiusException();

        mockMvc.perform(post("/api/companion-meetings/{meetingId}/check-in", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 40.0,
                                  "longitude": 2.0
                                }
                                """)
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("OUT_OF_CHECK_IN_RADIUS"))
                .andExpect(jsonPath("$.message").value("만남 인증 가능 반경 밖에 있습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsInvalidRequestWhenRequestBodyIsNull() throws Exception {
        useCase.exception = new InvalidCheckInRequestException();

        mockMvc.perform(post("/api/companion-meetings/{meetingId}/check-in", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null")
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_CHECK_IN_REQUEST"))
                .andExpect(jsonPath("$.message").value("올바르지 않은 만남 인증 요청입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));

        assertNull(useCase.command);
    }

    private CheckInCompanionMeetingResult result(final boolean alreadyCompleted) {
        return new CheckInCompanionMeetingResult(
                1L,
                CompanionMeetingStatus.ONGOING,
                true,
                2L,
                3L,
                false,
                false,
                LocalDateTime.of(2026, 6, 29, 18, 35),
                24.5,
                150.0,
                LocalDateTime.of(2026, 6, 29, 17, 30),
                LocalDateTime.of(2026, 6, 29, 19, 30),
                alreadyCompleted
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

    private static final class FakeCheckInCompanionMeetingUseCase implements CheckInCompanionMeetingUseCase {

        private CheckInCompanionMeetingResult result;
        private RuntimeException exception;
        private CheckInCompanionMeetingCommand command;

        @Override
        public CheckInCompanionMeetingResult checkIn(final CheckInCompanionMeetingCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
