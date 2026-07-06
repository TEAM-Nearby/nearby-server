// 동행 알림 컨트롤러의 요청 파싱과 응답 형식을 검증하는 테스트
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
import com.sopt.nearby.companion.application.MarkCompanionNotificationAsReadResult;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationHostProfile;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import com.sopt.nearby.companion.port.in.MarkCompanionNotificationAsReadUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionNotificationsUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompanionNotificationControllerTest {

    private MockMvc mockMvc;
    private FakeReadCompanionNotificationsUseCase useCase;
    private FakeMarkCompanionNotificationAsReadUseCase markUseCase;

    @BeforeEach
    void setUp() {
        useCase = new FakeReadCompanionNotificationsUseCase();
        markUseCase = new FakeMarkCompanionNotificationAsReadUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanionNotificationController(useCase, markUseCase))
                .setMessageConverters(jsonMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsSentNotificationsAndPassesAuthenticatedUserIdToUseCase() throws Exception {
        useCase.result = List.of(CompanionNotificationSummary.of(
                CompanionNotificationDirection.SENT,
                99L,
                1L,
                CompanionApplicationStatus.ACCEPTED,
                new CompanionNotificationHostProfile(100L, "https://image.example/host.png", "호스트"),
                "오노테라",
                LocalDateTime.of(2026, 6, 18, 18, 30),
                10L,
                false
        ));

        mockMvc.perform(get("/api/users/me/companion-requests")
                        .queryParam("direction", "SENT")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_COMPANION_REQUESTS"))
                .andExpect(jsonPath("$.message").value("동행 요청 목록을 조회했어요."))
                .andExpect(jsonPath("$.data.direction").value("SENT"))
                .andExpect(jsonPath("$.data.requests[0].notificationId").value(99))
                .andExpect(jsonPath("$.data.requests[0].applicationId").value(1))
                .andExpect(jsonPath("$.data.requests[0].applicationStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.requests[0].host.userId").value(100))
                .andExpect(jsonPath("$.data.requests[0].host.profileImageUrl")
                        .value("https://image.example/host.png"))
                .andExpect(jsonPath("$.data.requests[0].host.nickname").value("호스트"))
                .andExpect(jsonPath("$.data.requests[0].placeName").value("오노테라"))
                .andExpect(jsonPath("$.data.requests[0].meetingAt").value("2026-06-18T18:30:00"))
                .andExpect(jsonPath("$.data.requests[0].matchId").value(10))
                .andExpect(jsonPath("$.data.requests[0].actionType").value("CONFIRM_SCHEDULE"))
                .andExpect(jsonPath("$.data.requests[0].isRead").value(false));

        assertEquals(7L, useCase.userId);
        assertEquals(CompanionNotificationDirection.SENT, useCase.direction);
    }

    @Test
    void returnsReceivedNotificationsWithAcceptRequestActionType() throws Exception {
        useCase.result = List.of(CompanionNotificationSummary.of(
                CompanionNotificationDirection.RECEIVED,
                100L,
                2L,
                CompanionApplicationStatus.PENDING,
                new CompanionNotificationHostProfile(100L, null, "호스트"),
                "시우다드 콘달",
                LocalDateTime.of(2026, 6, 18, 19, 0),
                null,
                true
        ));

        mockMvc.perform(get("/api/users/me/companion-requests")
                        .queryParam("direction", "RECEIVED")
                        .principal(principal("100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.direction").value("RECEIVED"))
                .andExpect(jsonPath("$.data.requests[0].notificationId").value(100))
                .andExpect(jsonPath("$.data.requests[0].applicationId").value(2))
                .andExpect(jsonPath("$.data.requests[0].applicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.requests[0].host.profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[0].matchId").value(nullValue()))
                .andExpect(jsonPath("$.data.requests[0].actionType").value("ACCEPT_REQUEST"))
                .andExpect(jsonPath("$.data.requests[0].isRead").value(true));

        assertEquals(100L, useCase.userId);
        assertEquals(CompanionNotificationDirection.RECEIVED, useCase.direction);
    }

    @Test
    void returnsInvalidRequestDirectionWhenDirectionIsInvalid() throws Exception {
        mockMvc.perform(get("/api/users/me/companion-requests")
                        .queryParam("direction", "UNKNOWN")
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_DIRECTION"))
                .andExpect(jsonPath("$.message").value("올바르지 않은 요청 방향입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void marksNotificationAsReadAndPassesAuthenticatedUserIdToUseCase() throws Exception {
        markUseCase.result = new MarkCompanionNotificationAsReadResult(
                99L,
                true,
                LocalDateTime.of(2026, 7, 6, 20, 30)
        );

        mockMvc.perform(patch("/api/users/me/companion-requests/{notificationId}/read", 99L)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("MARK_COMPANION_NOTIFICATION_AS_READ"))
                .andExpect(jsonPath("$.message").value("동행 알림을 읽음 처리했어요."))
                .andExpect(jsonPath("$.data.notificationId").value(99))
                .andExpect(jsonPath("$.data.isRead").value(true))
                .andExpect(jsonPath("$.data.readAt").value("2026-07-06T20:30:00"));

        assertEquals(7L, markUseCase.userId);
        assertEquals(99L, markUseCase.notificationId);
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

    private static final class FakeReadCompanionNotificationsUseCase implements ReadCompanionNotificationsUseCase {

        private List<CompanionNotificationSummary> result = List.of();
        private Long userId;
        private CompanionNotificationDirection direction;

        @Override
        public List<CompanionNotificationSummary> getNotifications(
                final Long userId,
                final CompanionNotificationDirection direction
        ) {
            this.userId = userId;
            this.direction = direction;
            return result;
        }
    }

    private static final class FakeMarkCompanionNotificationAsReadUseCase
            implements MarkCompanionNotificationAsReadUseCase {

        private MarkCompanionNotificationAsReadResult result;
        private Long userId;
        private Long notificationId;

        @Override
        public MarkCompanionNotificationAsReadResult markAsRead(
                final Long userId,
                final Long notificationId
        ) {
            this.userId = userId;
            this.notificationId = notificationId;
            return result;
        }
    }
}
