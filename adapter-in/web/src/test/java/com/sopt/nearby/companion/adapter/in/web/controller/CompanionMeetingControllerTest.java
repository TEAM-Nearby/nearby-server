// 동행 만남 컨트롤러의 체크인 요청 파싱과 응답 형식을 검증하는 테스트
package com.sopt.nearby.companion.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sopt.nearby.companion.application.CheckInCompanionMeetingCommand;
import com.sopt.nearby.companion.application.CheckInCompanionMeetingResult;
import com.sopt.nearby.companion.application.CompleteCompanionMeetingResult;
import com.sopt.nearby.companion.application.CreateCompanionReviewsCommand;
import com.sopt.nearby.companion.application.CreateCompanionReviewsResult;
import com.sopt.nearby.companion.application.ReadCompanionMeetingDetailResult;
import com.sopt.nearby.companion.application.ReadCompanionReviewTargetsResult;
import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingCurrentUserNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompleteCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.InvalidCheckInRequestException;
import com.sopt.nearby.companion.domain.exception.OutOfCheckInRadiusException;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingProgressStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingHostProfile;
import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingSummary;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.review.CompanionReviewTarget;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.port.in.CheckInCompanionMeetingUseCase;
import com.sopt.nearby.companion.port.in.CompleteCompanionMeetingUseCase;
import com.sopt.nearby.companion.port.in.CreateCompanionReviewsUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionMeetingDetailUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionReviewTargetsUseCase;
import com.sopt.nearby.companion.port.in.ReadOngoingCompanionMeetingsUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompanionMeetingControllerTest {

    private MockMvc mockMvc;
    private FakeReadOngoingCompanionMeetingsUseCase ongoingReadUseCase;
    private FakeReadCompanionMeetingDetailUseCase detailReadUseCase;
    private FakeReadCompanionReviewTargetsUseCase reviewTargetsUseCase;
    private FakeCheckInCompanionMeetingUseCase useCase;
    private FakeCompleteCompanionMeetingUseCase completeUseCase;
    private FakeCreateCompanionReviewsUseCase reviewUseCase;

    @BeforeEach
    void setUp() {
        ongoingReadUseCase = new FakeReadOngoingCompanionMeetingsUseCase();
        detailReadUseCase = new FakeReadCompanionMeetingDetailUseCase();
        reviewTargetsUseCase = new FakeReadCompanionReviewTargetsUseCase();
        useCase = new FakeCheckInCompanionMeetingUseCase();
        completeUseCase = new FakeCompleteCompanionMeetingUseCase();
        reviewUseCase = new FakeCreateCompanionReviewsUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanionMeetingController(
                        ongoingReadUseCase,
                        detailReadUseCase,
                        reviewTargetsUseCase,
                        useCase,
                        completeUseCase,
                        reviewUseCase
                ))
                .setMessageConverters(jsonMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsOngoingMeetingsAndPassesAuthenticatedUserIdToUseCase() throws Exception {
        ongoingReadUseCase.result = List.of(ongoingMeeting(false), ongoingMeeting(true));

        mockMvc.perform(get("/api/companion-meetings")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_ONGOING_COMPANION_MEETINGS"))
                .andExpect(jsonPath("$.message").value("현재 진행 중인 동행 목록을 조회했어요."))
                .andExpect(jsonPath("$.data.meetings[0].meetingId").value(1))
                .andExpect(jsonPath("$.data.meetings[0].matchId").value(10))
                .andExpect(jsonPath("$.data.meetings[0].companion.userId").value(7))
                .andExpect(jsonPath("$.data.meetings[0].companion.profileImageUrl")
                        .value("https://image.url/profile.png"))
                .andExpect(jsonPath("$.data.meetings[0].companion.nickname").value("정지영"))
                .andExpect(jsonPath("$.data.meetings[0].companion.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.meetings[0].placeName").value("시우다드 콘달"))
                .andExpect(jsonPath("$.data.meetings[0].meetingAt").value("2026-06-29T16:30:00"))
                .andExpect(jsonPath("$.data.meetings[0].meetingTimeType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.meetings[0].isCheckedIn").value(false))
                .andExpect(jsonPath("$.data.meetings[0].meetingStatus").value("ONGOING"))
                .andExpect(jsonPath("$.data.meetings[1].isCheckedIn").value(true));

        assertEquals(7L, ongoingReadUseCase.userId);
    }

    @Test
    void returnsEmptyOngoingMeetingsWhenUseCaseReturnsNoMeetings() throws Exception {
        ongoingReadUseCase.result = List.of();

        mockMvc.perform(get("/api/companion-meetings")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_ONGOING_COMPANION_MEETINGS"))
                .andExpect(jsonPath("$.data.meetings").isArray())
                .andExpect(jsonPath("$.data.meetings").isEmpty());
    }

    @Test
    void returnsSchedulingMeetingWithoutMeetingDetails() throws Exception {
        ongoingReadUseCase.result = List.of(schedulingMeeting());

        mockMvc.perform(get("/api/companion-meetings")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meetings[0].meetingId").value(nullValue()))
                .andExpect(jsonPath("$.data.meetings[0].meetingAt").value(nullValue()))
                .andExpect(jsonPath("$.data.meetings[0].meetingStatus").value(nullValue()))
                .andExpect(jsonPath("$.data.meetings[0].progressStatus").value("SCHEDULING"));
    }

    @Test
    void returnsOngoingMeetingDetailAndPassesAuthenticatedUserIdToUseCase() throws Exception {
        detailReadUseCase.result = detailResult(MatchParticipantRole.GUEST);

        mockMvc.perform(get("/api/companion-meetings/{meetingId}", 1L)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_COMPANION_MEETING_DETAIL"))
                .andExpect(jsonPath("$.message").value("진행 중인 동행 상세 정보를 조회했어요."))
                .andExpect(jsonPath("$.data.meetingId").value(1))
                .andExpect(jsonPath("$.data.currentUserRole").value("GUEST"))
                .andExpect(jsonPath("$.data.hostId").value(1))
                .andExpect(jsonPath("$.data.hostGender").value("FEMALE"))
                .andExpect(jsonPath("$.data.hostProfileImageUrl").value("https://image.url/profile.png"))
                .andExpect(jsonPath("$.data.hostNickname").value("정지영"))
                .andExpect(jsonPath("$.data.hostCheckedIn").value(true))
                .andExpect(jsonPath("$.data.placeName").value("시우다드 콘달"))
                .andExpect(jsonPath("$.data.meetingAt").value("2026-06-29T18:30:00"))
                .andExpect(jsonPath("$.data.meetingTimeType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.meetingStatus").value("ONGOING"))
                .andExpect(jsonPath("$.data.currentUserCheckedIn").value(false))
                .andExpect(jsonPath("$.data.canCancelMeeting").value(true));

        assertEquals(1L, detailReadUseCase.meetingId);
        assertEquals(7L, detailReadUseCase.userId);
    }

    @Test
    void returnsHostProfileEvenWhenRequesterIsHost() throws Exception {
        detailReadUseCase.result = detailResult(MatchParticipantRole.HOST);

        mockMvc.perform(get("/api/companion-meetings/{meetingId}", 1L)
                        .principal(principal("1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentUserRole").value("HOST"))
                .andExpect(jsonPath("$.data.hostId").value(1))
                .andExpect(jsonPath("$.data.hostGender").value("FEMALE"));
    }

    @Test
    void returnsReviewTargetsAndPassesAuthenticatedUserIdToUseCase() throws Exception {
        reviewTargetsUseCase.result = new ReadCompanionReviewTargetsResult(
                CompanionMeetingStatus.COMPLETED,
                MatchParticipantRole.HOST,
                false,
                List.of(
                        reviewTarget(2L, "조예원", false),
                        reviewTarget(3L, "김솝트", true)
                )
        );

        mockMvc.perform(get("/api/companion-meetings/{meetingId}/review-targets", 1L)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_COMPANION_REVIEW_TARGETS"))
                .andExpect(jsonPath("$.message").value("동행 후기 대상 목록을 조회했어요."))
                .andExpect(jsonPath("$.data.meetingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.currentUserRole").value("HOST"))
                .andExpect(jsonPath("$.data.canCompleteMeeting").value(false))
                .andExpect(jsonPath("$.data.reviewTargets[0].revieweeUserId").value(2))
                .andExpect(jsonPath("$.data.reviewTargets[0].profileImageUrl")
                        .value("https://image.url/profile-2.png"))
                .andExpect(jsonPath("$.data.reviewTargets[0].nickname").value("조예원"))
                .andExpect(jsonPath("$.data.reviewTargets[0].cityName").value("바르셀로나"))
                .andExpect(jsonPath("$.data.reviewTargets[0].meetingDate").value("2026-06-18"))
                .andExpect(jsonPath("$.data.reviewTargets[0].isCheckedIn").value(true))
                .andExpect(jsonPath("$.data.reviewTargets[0].hasWrittenReview").value(false))
                .andExpect(jsonPath("$.data.reviewTargets[1].hasWrittenReview").value(true));

        assertEquals(1L, reviewTargetsUseCase.meetingId);
        assertEquals(7L, reviewTargetsUseCase.userId);
    }

    @Test
    void returnsEmptyReviewTargets() throws Exception {
        reviewTargetsUseCase.result = new ReadCompanionReviewTargetsResult(
                CompanionMeetingStatus.COMPLETED,
                MatchParticipantRole.HOST,
                false,
                List.of()
        );

        mockMvc.perform(get("/api/companion-meetings/{meetingId}/review-targets", 1L)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewTargets").isArray())
                .andExpect(jsonPath("$.data.reviewTargets").isEmpty());
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
    void completesCompanionMeetingAndPassesAuthenticatedUserIdToUseCase() throws Exception {
        completeUseCase.result = new CompleteCompanionMeetingResult(
                1L,
                10L,
                true,
                LocalDateTime.of(2026, 6, 29, 19, 0),
                CompanionMeetingStatus.ONGOING,
                null
        );

        mockMvc.perform(patch("/api/companion-meetings/{meetingId}/complete", 1L)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMPLETE_COMPANION_MEETING"))
                .andExpect(jsonPath("$.message").value("동행 마치기가 반영되었어요."))
                .andExpect(jsonPath("$.data.meetingId").value(1))
                .andExpect(jsonPath("$.data.matchId").value(10))
                .andExpect(jsonPath("$.data.currentUserCompleted").value(true))
                .andExpect(jsonPath("$.data.currentUserCompletedAt").value("2026-06-29T19:00:00"))
                .andExpect(jsonPath("$.data.meetingStatus").value("ONGOING"))
                .andExpect(jsonPath("$.data.meetingCompletedAt").value(nullValue()));

        assertEquals(1L, completeUseCase.meetingId);
        assertEquals(7L, completeUseCase.userId);
    }

    @Test
    void returnsForbiddenErrorWhenCompletingMeetingAsNonParticipant() throws Exception {
        completeUseCase.exception = new ForbiddenCompleteCompanionMeetingException();

        mockMvc.perform(patch("/api/companion-meetings/{meetingId}/complete", 1L)
                        .principal(principal("7")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN_COMPLETE_COMPANION_MEETING"))
                .andExpect(jsonPath("$.message").value("해당 동행의 참여자만 동행을 마칠 수 있습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsAlreadyCanceledErrorWhenCompletingCanceledMeeting() throws Exception {
        completeUseCase.exception = new CompleteCompanionMeetingAlreadyCanceledException();

        mockMvc.perform(patch("/api/companion-meetings/{meetingId}/complete", 1L)
                        .principal(principal("7")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMPLETE_COMPANION_MEETING_ALREADY_CANCELED"))
                .andExpect(jsonPath("$.message").value("취소된 동행은 완료할 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsAlreadyCompletedErrorWhenCompletingCompletedMeeting() throws Exception {
        completeUseCase.exception = new CompleteCompanionMeetingAlreadyCompletedException();

        mockMvc.perform(patch("/api/companion-meetings/{meetingId}/complete", 1L)
                        .principal(principal("7")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMPLETE_COMPANION_MEETING_ALREADY_COMPLETED"))
                .andExpect(jsonPath("$.message").value("이미 완료된 동행입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsCurrentUserNotCheckedInErrorWhenCompletingMeeting() throws Exception {
        completeUseCase.exception = new CompleteCompanionMeetingCurrentUserNotCheckedInException();

        mockMvc.perform(patch("/api/companion-meetings/{meetingId}/complete", 1L)
                        .principal(principal("7")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMPLETE_COMPANION_MEETING_CURRENT_USER_NOT_CHECKED_IN"))
                .andExpect(jsonPath("$.message").value("만남 인증을 완료한 후 동행을 마칠 수 있습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
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

    @Test
    void createsCompanionReviewsAndPassesAuthenticatedUserIdToUseCase() throws Exception {
        reviewUseCase.result = new CreateCompanionReviewsResult(
                1L,
                10L,
                CompanionMeetingStatus.ONGOING
        );

        mockMvc.perform(post("/api/companion-meetings/{meetingId}/reviews", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "revieweeUserId": 2,
                                  "rating": 5,
                                  "keywords": [
                                    "FAST_RESPONSE",
                                    "GOOD_MANNERS",
                                    "PUNCTUAL"
                                  ]
                                }
                                """)
                        .principal(principal("7")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("CREATE_COMPANION_REVIEWS"))
                .andExpect(jsonPath("$.message").value("동행 후기가 등록되었어요."))
                .andExpect(jsonPath("$.data.meetingId").value(1))
                .andExpect(jsonPath("$.data.reviewId").value(10))
                .andExpect(jsonPath("$.data.meetingStatus").value("ONGOING"));

        assertEquals(7L, reviewUseCase.command.reviewerUserId());
        assertEquals(1L, reviewUseCase.command.meetingId());
        assertEquals(2L, reviewUseCase.command.revieweeUserId());
        assertEquals(5, reviewUseCase.command.rating());
        assertIterableEquals(
                List.of(ReviewKeyword.FAST_RESPONSE, ReviewKeyword.GOOD_MANNERS, ReviewKeyword.PUNCTUAL),
                reviewUseCase.command.keywords()
        );
    }

    @Test
    void returnsInvalidReviewKeywordWhenKeywordIsUnknown() throws Exception {
        mockMvc.perform(post("/api/companion-meetings/{meetingId}/reviews", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "revieweeUserId": 2,
                                  "rating": 5,
                                  "keywords": [
                                    "UNKNOWN"
                                  ]
                                }
                                """)
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_KEYWORD"))
                .andExpect(jsonPath("$.message").value("올바르지 않은 후기 키워드입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));

        assertNull(reviewUseCase.command);
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

    private OngoingCompanionMeetingSummary ongoingMeeting(final boolean checkedIn) {
        return new OngoingCompanionMeetingSummary(
                checkedIn ? 2L : 1L,
                checkedIn ? 20L : 10L,
                new OngoingCompanionMeetingHostProfile(
                        7L,
                        "https://image.url/profile.png",
                        "정지영",
                        UserGender.FEMALE
                ),
                "시우다드 콘달",
                LocalDateTime.of(2026, 6, 29, 16, 30),
                CompanionPostMeetingTimeType.SCHEDULED,
                checkedIn,
                CompanionMeetingStatus.ONGOING,
                CompanionMeetingProgressStatus.ONGOING
        );
    }

    private OngoingCompanionMeetingSummary schedulingMeeting() {
        return new OngoingCompanionMeetingSummary(
                null,
                10L,
                new OngoingCompanionMeetingHostProfile(
                        7L,
                        "https://image.url/profile.png",
                        "정지영",
                        UserGender.FEMALE
                ),
                "시우다드 콘달",
                null,
                CompanionPostMeetingTimeType.UNDECIDED,
                false,
                null,
                CompanionMeetingProgressStatus.SCHEDULING
        );
    }

    private ReadCompanionMeetingDetailResult detailResult(final MatchParticipantRole currentUserRole) {
        return new ReadCompanionMeetingDetailResult(
                1L,
                currentUserRole,
                1L,
                UserGender.FEMALE,
                "https://image.url/profile.png",
                "정지영",
                true,
                "시우다드 콘달",
                LocalDateTime.of(2026, 6, 29, 18, 30),
                CompanionPostMeetingTimeType.SCHEDULED,
                CompanionMeetingStatus.ONGOING,
                false,
                true
        );
    }

    private CompanionReviewTarget reviewTarget(
            final Long revieweeUserId,
            final String nickname,
            final boolean hasWrittenReview
    ) {
        return new CompanionReviewTarget(
                revieweeUserId,
                "https://image.url/profile-" + revieweeUserId + ".png",
                nickname,
                "바르셀로나",
                LocalDate.of(2026, 6, 18),
                true,
                hasWrittenReview
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

    private static final class FakeReadCompanionMeetingDetailUseCase implements ReadCompanionMeetingDetailUseCase {

        private ReadCompanionMeetingDetailResult result;
        private Long meetingId;
        private Long userId;

        @Override
        public ReadCompanionMeetingDetailResult getDetail(final Long meetingId, final Long userId) {
            this.meetingId = meetingId;
            this.userId = userId;
            return result;
        }
    }

    private static final class FakeReadOngoingCompanionMeetingsUseCase implements ReadOngoingCompanionMeetingsUseCase {

        private Long userId;
        private List<OngoingCompanionMeetingSummary> result = List.of();

        @Override
        public List<OngoingCompanionMeetingSummary> getOngoingMeetings(final Long userId) {
            this.userId = userId;
            return result;
        }
    }

    private static final class FakeReadCompanionReviewTargetsUseCase implements ReadCompanionReviewTargetsUseCase {

        private ReadCompanionReviewTargetsResult result;
        private Long meetingId;
        private Long userId;

        @Override
        public ReadCompanionReviewTargetsResult getTargets(final Long meetingId, final Long userId) {
            this.meetingId = meetingId;
            this.userId = userId;
            return result;
        }
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

    private static final class FakeCompleteCompanionMeetingUseCase implements CompleteCompanionMeetingUseCase {

        private CompleteCompanionMeetingResult result;
        private RuntimeException exception;
        private Long meetingId;
        private Long userId;

        @Override
        public CompleteCompanionMeetingResult complete(final Long meetingId, final Long userId) {
            this.meetingId = meetingId;
            this.userId = userId;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }

    private static final class FakeCreateCompanionReviewsUseCase implements CreateCompanionReviewsUseCase {

        private CreateCompanionReviewsResult result;
        private RuntimeException exception;
        private CreateCompanionReviewsCommand command;

        @Override
        public CreateCompanionReviewsResult create(final CreateCompanionReviewsCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
