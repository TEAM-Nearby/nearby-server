// 동행 모집글 목록 조회 컨트롤러의 요청 파싱과 응답 형식을 검증한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sopt.nearby.companion.application.CreateCompanionPostCommand;
import com.sopt.nearby.companion.application.CreateCompanionPostResult;
import com.sopt.nearby.companion.application.CreateCompanionRequestCommand;
import com.sopt.nearby.companion.application.CreateCompanionRequestResult;
import com.sopt.nearby.companion.application.CompanionPostDetailResult;
import com.sopt.nearby.companion.application.CompanionPostsResult;
import com.sopt.nearby.companion.application.ReadCompanionPostDetailCommand;
import com.sopt.nearby.companion.application.ReadCompanionPostsCommand;
import com.sopt.nearby.companion.domain.exception.CompanionPostExpiredException;
import com.sopt.nearby.companion.domain.exception.CompanionPostNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionPostNotRecruitingException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestAlreadyExistsException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionPostCreateRequestException;
import com.sopt.nearby.companion.domain.exception.InvalidOpenChatUrlException;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostApplyStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostKeyword;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostSort;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.in.CreateCompanionPostUseCase;
import com.sopt.nearby.companion.port.in.CreateCompanionRequestUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionPostDetailUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionPostsUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import com.sopt.nearby.user.exception.OnboardingRequiredException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompanionPostControllerTest {

    private MockMvc mockMvc;
    private FakeReadCompanionPostsUseCase readUseCase;
    private FakeCreateCompanionPostUseCase createUseCase;
    private FakeReadCompanionPostDetailUseCase detailUseCase;
    private FakeCreateCompanionRequestUseCase createRequestUseCase;

    @BeforeEach
    void setUp() {
        readUseCase = new FakeReadCompanionPostsUseCase();
        createUseCase = new FakeCreateCompanionPostUseCase();
        detailUseCase = new FakeReadCompanionPostDetailUseCase();
        createRequestUseCase = new FakeCreateCompanionRequestUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanionPostController(
                        readUseCase,
                        createUseCase,
                        detailUseCase,
                        createRequestUseCase
                ))
                .setMessageConverters(jsonMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void passesDefaultsAndAuthenticatedUserIdToUseCase() throws Exception {
        readUseCase.result = result(1000, CompanionPostPlaceCategory.ALL, CompanionPostSort.LATEST);

        mockMvc.perform(get("/api/companion-posts")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMPANION_POSTS_FOUND"))
                .andExpect(jsonPath("$.message").value("동행 모집 글 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.currentLocation.latitude").value(37.56650000))
                .andExpect(jsonPath("$.data.currentLocation.longitude").value(126.97800000))
                .andExpect(jsonPath("$.data.radiusMeters").value(1000))
                .andExpect(jsonPath("$.data.maxRadiusMeters").value(5000))
                .andExpect(jsonPath("$.data.placeCategory").value("ALL"))
                .andExpect(jsonPath("$.data.sort").value("LATEST"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.summaryText").value("내 주변 1개의 동행이 있어요"))
                .andExpect(jsonPath("$.data.posts[0].postId").value(101))
                .andExpect(jsonPath("$.data.posts[0].status").value("RECRUITING"))
                .andExpect(jsonPath("$.data.posts[0].host.nickname").value("니어바이"))
                .andExpect(jsonPath("$.data.posts[0].host.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.posts[0].place.placeId").value(20))
                .andExpect(jsonPath("$.data.posts[0].place.googlePlaceId").value("google-place-id"))
                .andExpect(jsonPath("$.data.posts[0].place.name").value("니어바이스시"))
                .andExpect(jsonPath("$.data.posts[0].place.category").value("RESTAURANT"))
                .andExpect(jsonPath("$.data.posts[0].place.distanceMeters").value(320))
                .andExpect(jsonPath("$.data.posts[0].place.imageSource").value("GOOGLE_MAPS"))
                .andExpect(jsonPath("$.data.posts[0].place.imageAttributions").isArray())
                .andExpect(jsonPath("$.data.posts[0].contentPreview").value("같이 스시 먹어요"))
                .andExpect(jsonPath("$.data.posts[0].contentPreviewTruncated").value(false))
                .andExpect(jsonPath("$.data.posts[0].meetingAt").value("2026-07-03T14:00:00"))
                .andExpect(jsonPath("$.data.posts[0].meetingAtText").value("7월 3일 14:00"))
                .andExpect(jsonPath("$.data.posts[0].participantCount").value(2))
                .andExpect(jsonPath("$.data.posts[0].maxParticipants").value(4))
                .andExpect(jsonPath("$.data.posts[0].participantSummaryText").value("2/4 모집 중"))
                .andExpect(jsonPath("$.data.posts[0].createdAt").value("2026-07-02T13:30:00"))
                .andExpect(jsonPath("$.data.posts[0].createdAgoText").value("30분 전"))
                .andExpect(jsonPath("$.data.posts[0].mapMarkerText").value("7월 3일 14시 니어바이스시 동행"));

        assertEquals(7L, readUseCase.command.userId());
        assertEquals(new BigDecimal("37.56650000"), readUseCase.command.latitude());
        assertEquals(new BigDecimal("126.97800000"), readUseCase.command.longitude());
        assertEquals(1000, readUseCase.command.radiusMeters());
        assertEquals(CompanionPostPlaceCategory.ALL, readUseCase.command.placeCategory());
        assertEquals(CompanionPostSort.LATEST, readUseCase.command.sort());
    }

    @Test
    void passesExplicitFiltersToUseCase() throws Exception {
        readUseCase.result = result(3000, CompanionPostPlaceCategory.CAFE, CompanionPostSort.DISTANCE);

        mockMvc.perform(get("/api/companion-posts")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .queryParam("radiusMeters", "3000")
                        .queryParam("placeCategory", "CAFE")
                        .queryParam("placeId", "20")
                        .queryParam("sort", "DISTANCE")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.radiusMeters").value(3000))
                .andExpect(jsonPath("$.data.placeCategory").value("CAFE"))
                .andExpect(jsonPath("$.data.sort").value("DISTANCE"));

        assertEquals(3000, readUseCase.command.radiusMeters());
        assertEquals(CompanionPostPlaceCategory.CAFE, readUseCase.command.placeCategory());
        assertEquals(20L, readUseCase.command.placeId());
        assertEquals(CompanionPostSort.DISTANCE, readUseCase.command.sort());
    }

    @Test
    void returnsValidationErrorForInvalidQuery() throws Exception {
        mockMvc.perform(get("/api/companion-posts")
                        .queryParam("latitude", "invalid")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("위도, 경도, 반경, 카테고리, 정렬 기준이 올바르지 않습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsOnboardingRequired() throws Exception {
        readUseCase.exception = new OnboardingRequiredException();

        mockMvc.perform(get("/api/companion-posts")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"))
                .andExpect(jsonPath("$.message").value("온보딩 과정이 완료되지 않았습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void createsCompanionPost() throws Exception {
        createUseCase.result = createResult(
                CompanionPostMeetingTimeType.SCHEDULED,
                LocalDateTime.of(2026, 7, 3, 14, 0),
                null,
                true,
                List.of(CompanionPostKeyword.NEW_FOOD_CHALLENGE, CompanionPostKeyword.PHOTO_LOVER),
                CompanionPostPlaceCategory.RESTAURANT
        );

        mockMvc.perform(post("/api/companion-posts")
                        .contentType("application/json")
                        .content("""
                                {
                                  "place": {
                                    "googlePlaceId": "google-place-id",
                                    "name": "니어바이 스시",
                                    "address": "서울특별시 중구 세종대로 110",
                                    "latitude": 37.5671,
                                    "longitude": 126.9792,
                                    "category": "RESTAURANT"
                                  },
                                  "meetingTimeType": "SCHEDULED",
                                  "meetingAt": "2026-07-03T14:00:00",
                                  "maxParticipants": 4,
                                  "departEvenIfNotFull": true,
                                  "styleKeywords": ["NEW_FOOD_CHALLENGE", "PHOTO_LOVER"],
                                  "content": "같이 스시 먹으러 갈 사람 구해요.",
                                  "openChatUrl": "https://open.kakao.com/o/nearby123"
                                }
                                """)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMPANION_POST_CREATED"))
                .andExpect(jsonPath("$.message").value("동행 모집 글 작성에 성공했습니다."))
                .andExpect(jsonPath("$.data.postId").value(101))
                .andExpect(jsonPath("$.data.status").value("RECRUITING"))
                .andExpect(jsonPath("$.data.hostUserId").value(7))
                .andExpect(jsonPath("$.data.place.placeId").value(20))
                .andExpect(jsonPath("$.data.place.googlePlaceId").value("google-place-id"))
                .andExpect(jsonPath("$.data.place.name").value("니어바이 스시"))
                .andExpect(jsonPath("$.data.place.address").value("서울특별시 중구 세종대로 110"))
                .andExpect(jsonPath("$.data.place.latitude").value(37.5671))
                .andExpect(jsonPath("$.data.place.longitude").value(126.9792))
                .andExpect(jsonPath("$.data.place.category").value("RESTAURANT"))
                .andExpect(jsonPath("$.data.meetingTimeType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.meetingAt").value("2026-07-03T14:00:00"))
                .andExpect(jsonPath("$.data.exposureExpiresAt").value(nullValue()))
                .andExpect(jsonPath("$.data.maxParticipants").value(4))
                .andExpect(jsonPath("$.data.participantCount").value(1))
                .andExpect(jsonPath("$.data.departEvenIfNotFull").value(true))
                .andExpect(jsonPath("$.data.styleKeywords[0]").value("NEW_FOOD_CHALLENGE"))
                .andExpect(jsonPath("$.data.styleKeywords[1]").value("PHOTO_LOVER"))
                .andExpect(jsonPath("$.data.content").value("같이 스시 먹으러 갈 사람 구해요."))
                .andExpect(jsonPath("$.data.openChatUrl").value("https://open.kakao.com/o/nearby123"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-07-02T14:00:00"));

        assertEquals(7L, createUseCase.command.hostUserId());
        assertEquals("google-place-id", createUseCase.command.place().googlePlaceId());
        assertEquals(CompanionPostPlaceCategory.RESTAURANT, createUseCase.command.place().category());
        assertEquals(CompanionPostMeetingTimeType.SCHEDULED, createUseCase.command.meetingTimeType());
        assertEquals(LocalDateTime.of(2026, 7, 3, 14, 0), createUseCase.command.meetingAt());
        assertEquals(4, createUseCase.command.maxParticipants());
        assertEquals(true, createUseCase.command.departEvenIfNotFull());
        assertEquals(
                List.of(CompanionPostKeyword.NEW_FOOD_CHALLENGE, CompanionPostKeyword.PHOTO_LOVER),
                createUseCase.command.styleKeywords()
        );
    }

    @Test
    void createsCompanionPostWithDefaults() throws Exception {
        createUseCase.result = createResult(
                CompanionPostMeetingTimeType.NOW,
                null,
                LocalDateTime.of(2026, 7, 2, 15, 0),
                true,
                List.of(),
                CompanionPostPlaceCategory.OTHER
        );

        mockMvc.perform(post("/api/companion-posts")
                        .contentType("application/json")
                        .content("""
                                {
                                  "place": {
                                    "googlePlaceId": "google-place-id",
                                    "name": "니어바이 스시",
                                    "latitude": 37.5671,
                                    "longitude": 126.9792
                                  },
                                  "meetingTimeType": "NOW",
                                  "maxParticipants": 4,
                                  "content": "같이 스시 먹으러 갈 사람 구해요.",
                                  "openChatUrl": "https://open.kakao.com/o/nearby123"
                                }
                                """)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meetingTimeType").value("NOW"))
                .andExpect(jsonPath("$.data.meetingAt").value(nullValue()))
                .andExpect(jsonPath("$.data.exposureExpiresAt").value("2026-07-02T15:00:00"))
                .andExpect(jsonPath("$.data.departEvenIfNotFull").value(true))
                .andExpect(jsonPath("$.data.styleKeywords").isArray())
                .andExpect(jsonPath("$.data.place.category").value("OTHER"));

        assertEquals(null, createUseCase.command.departEvenIfNotFull());
        assertEquals(null, createUseCase.command.styleKeywords());
        assertEquals(null, createUseCase.command.place().category());
    }

    @Test
    void returnsValidationErrorForInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/companion-posts")
                        .contentType("application/json")
                        .content("""
                                {
                                  "place": {
                                    "googlePlaceId": "google-place-id",
                                    "name": "니어바이 스시",
                                    "latitude": 37.5671,
                                    "longitude": 126.9792
                                  },
                                  "meetingTimeType": "INVALID",
                                  "maxParticipants": 4,
                                  "content": "같이 스시 먹으러 갈 사람 구해요.",
                                  "openChatUrl": "https://open.kakao.com/o/nearby123"
                                }
                                """)
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("필수값 누락, 인원 범위 오류, 소개 글자 수 초과, 장소 좌표 오류, 만남 시간 입력 규칙 위반입니다."));
    }

    @Test
    void returnsValidationErrorForNullCreateRequestBody() throws Exception {
        mockMvc.perform(post("/api/companion-posts")
                        .contentType("application/json")
                        .content("null")
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("필수값 누락, 인원 범위 오류, 소개 글자 수 초과, 장소 좌표 오류, 만남 시간 입력 규칙 위반입니다."));

        assertEquals(null, createUseCase.command);
    }

    @Test
    void returnsValidationErrorForEmptyCreateRequestBody() throws Exception {
        mockMvc.perform(post("/api/companion-posts")
                        .contentType("application/json")
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("필수값 누락, 인원 범위 오류, 소개 글자 수 초과, 장소 좌표 오류, 만남 시간 입력 규칙 위반입니다."));

        assertEquals(null, createUseCase.command);
    }

    @Test
    void returnsInvalidOpenChatUrl() throws Exception {
        createUseCase.exception = new InvalidOpenChatUrlException();

        mockMvc.perform(post("/api/companion-posts")
                        .contentType("application/json")
                        .content("""
                                {
                                  "place": {
                                    "googlePlaceId": "google-place-id",
                                    "name": "니어바이 스시",
                                    "latitude": 37.5671,
                                    "longitude": 126.9792
                                  },
                                  "meetingTimeType": "NOW",
                                  "maxParticipants": 4,
                                  "content": "같이 스시 먹으러 갈 사람 구해요.",
                                  "openChatUrl": "https://example.com/o/nearby123"
                                }
                                """)
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OPEN_CHAT_URL"))
                .andExpect(jsonPath("$.message").value("카카오톡 오픈채팅 링크 형식이 올바르지 않습니다."));
    }

    @Test
    void createsCompanionRequest() throws Exception {
        createRequestUseCase.result = new CreateCompanionRequestResult(
                1L,
                10L,
                CompanionApplicationStatus.PENDING,
                LocalDateTime.of(2026, 7, 15, 12, 30)
        );

        mockMvc.perform(post("/api/companion-posts/{postId}/companion-requests", 10L)
                        .principal(principal("7")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("CREATE_COMPANION_REQUEST"))
                .andExpect(jsonPath("$.message").value("동행 신청이 완료되었어요."))
                .andExpect(jsonPath("$.data.applicationId").value(1))
                .andExpect(jsonPath("$.data.postId").value(10))
                .andExpect(jsonPath("$.data.applicationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-07-15T12:30:00"));

        assertEquals(7L, createRequestUseCase.command.applicantUserId());
        assertEquals(10L, createRequestUseCase.command.postId());
    }

    @Test
    void returnsNotFoundWhenCompanionRequestPostIsMissing() throws Exception {
        createRequestUseCase.exception = new CompanionPostNotFoundException();

        mockMvc.perform(post("/api/companion-posts/{postId}/companion-requests", 10L)
                        .principal(principal("7")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMPANION_POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("동행 모집글을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsConflictWhenCompanionRequestAlreadyExists() throws Exception {
        createRequestUseCase.exception = new CompanionRequestAlreadyExistsException();

        mockMvc.perform(post("/api/companion-posts/{postId}/companion-requests", 10L)
                        .principal(principal("7")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMPANION_REQUEST_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 신청한 동행입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsConflictWhenCompanionPostIsNotRecruiting() throws Exception {
        createRequestUseCase.exception = new CompanionPostNotRecruitingException();

        mockMvc.perform(post("/api/companion-posts/{postId}/companion-requests", 10L)
                        .principal(principal("7")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("COMPANION_POST_NOT_RECRUITING"))
                .andExpect(jsonPath("$.message").value("모집 중인 동행이 아닙니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void getsCompanionPostDetail() throws Exception {
        detailUseCase.result = detailResult(null, CompanionPostApplyStatus.NOT_APPLIED);

        mockMvc.perform(get("/api/companion-posts/101")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMPANION_POST_FOUND"))
                .andExpect(jsonPath("$.message").value("동행 모집 글 상세 조회에 성공했어요."))
                .andExpect(jsonPath("$.data.postId").value(101))
                .andExpect(jsonPath("$.data.hostUserId").value(1))
                .andExpect(jsonPath("$.data.hostProfileId").value(5))
                .andExpect(jsonPath("$.data.googlePlaceId").value("google-place-id"))
                .andExpect(jsonPath("$.data.meetingAt").value("2026-07-03T14:00:00"))
                .andExpect(jsonPath("$.data.maxParticipants").value(4))
                .andExpect(jsonPath("$.data.content").value("같이 스시 먹으러 갈 사람 구해요."))
                .andExpect(jsonPath("$.data.openChatUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.status").value("RECRUITING"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-07-02T13:30:00"))
                .andExpect(jsonPath("$.data.meetingTimeType").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.expiresAt").value(nullValue()))
                .andExpect(jsonPath("$.data.participantCount").value(2))
                .andExpect(jsonPath("$.data.applyStatus").value("NOT_APPLIED"))
                .andExpect(jsonPath("$.data.place.name").value("니어바이 스시"))
                .andExpect(jsonPath("$.data.place.address").value("서울시 어딘가"))
                .andExpect(jsonPath("$.data.place.latitude").value(37.5671))
                .andExpect(jsonPath("$.data.place.longitude").value(126.9792))
                .andExpect(jsonPath("$.data.place.category").value("RESTAURANT"))
                .andExpect(jsonPath("$.data.hostProfileSummary.profileId").value(5))
                .andExpect(jsonPath("$.data.hostProfileSummary.nickname").value("니어바이"))
                .andExpect(jsonPath("$.data.hostProfileSummary.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.hostProfileSummary.birthYear").value(2001))
                .andExpect(jsonPath("$.data.hostProfileSummary.profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.hostProfileSummary.mannerScore").value(4.0))
                .andExpect(jsonPath("$.data.hostProfileSummary.phoneVerifiedAt").value("2026-07-01T10:00:00"))
                .andExpect(jsonPath("$.data.hostProfileSummary.keywords[0]").value("PLANNED"))
                .andExpect(jsonPath("$.data.hostProfileSummary.keywords[1]").value("FOODIE"));

        assertEquals(7L, detailUseCase.command.userId());
        assertEquals(101L, detailUseCase.command.postId());
    }

    @Test
    void getsCompanionPostDetailWithOpenChatUrl() throws Exception {
        detailUseCase.result = detailResult(
                "https://open.kakao.com/o/nearby123",
                CompanionPostApplyStatus.ACCEPTED
        );

        mockMvc.perform(get("/api/companion-posts/101")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openChatUrl").value("https://open.kakao.com/o/nearby123"))
                .andExpect(jsonPath("$.data.applyStatus").value("ACCEPTED"));
    }

    @Test
    void returnsNotFoundForMissingCompanionPostDetail() throws Exception {
        detailUseCase.exception = new CompanionPostNotFoundException();

        mockMvc.perform(get("/api/companion-posts/101")
                        .principal(principal("7")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMPANION_POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("동행 모집글을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsGoneForExpiredCompanionPostDetail() throws Exception {
        detailUseCase.exception = new CompanionPostExpiredException();

        mockMvc.perform(get("/api/companion-posts/101")
                        .principal(principal("7")))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410))
                .andExpect(jsonPath("$.code").value("COMPANION_POST_EXPIRED"))
                .andExpect(jsonPath("$.message").value("마감된 글입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private CompanionPostsResult result(
            final int radiusMeters,
            final CompanionPostPlaceCategory placeCategory,
            final CompanionPostSort sort
    ) {
        return new CompanionPostsResult(
                new CompanionPostsResult.CurrentLocation(
                        new BigDecimal("37.56650000"),
                        new BigDecimal("126.97800000")
                ),
                radiusMeters,
                5000,
                placeCategory,
                sort,
                1,
                "내 주변 1개의 동행이 있어요",
                List.of(new CompanionPostsResult.Post(
                        101L,
                        CompanionPostStatus.RECRUITING,
                        new CompanionPostsResult.Host("니어바이", UserGender.FEMALE),
                        new CompanionPostsResult.Place(
                                20L,
                                "google-place-id",
                                "니어바이스시",
                                CompanionPostPlaceCategory.RESTAURANT,
                                new BigDecimal("37.56710000"),
                                new BigDecimal("126.97920000"),
                                320,
                                "https://lh3.googleusercontent.com/place.jpg",
                                "GOOGLE_MAPS",
                                List.of()
                        ),
                        "같이 스시 먹어요",
                        false,
                        LocalDateTime.of(2026, 7, 3, 14, 0),
                        "7월 3일 14:00",
                        2,
                        4,
                        "2/4 모집 중",
                        LocalDateTime.of(2026, 7, 2, 13, 30),
                        "30분 전",
                        "7월 3일 14시 니어바이스시 동행"
                ))
        );
    }

    private CreateCompanionPostResult createResult(
            final CompanionPostMeetingTimeType meetingTimeType,
            final LocalDateTime meetingAt,
            final LocalDateTime exposureExpiresAt,
            final boolean departEvenIfNotFull,
            final List<CompanionPostKeyword> styleKeywords,
            final CompanionPostPlaceCategory category
    ) {
        return new CreateCompanionPostResult(
                101L,
                CompanionPostStatus.RECRUITING,
                7L,
                new CreateCompanionPostResult.Place(
                        20L,
                        "google-place-id",
                        "니어바이 스시",
                        "서울특별시 중구 세종대로 110",
                        new BigDecimal("37.5671"),
                        new BigDecimal("126.9792"),
                        category
                ),
                meetingTimeType,
                meetingAt,
                exposureExpiresAt,
                4,
                1,
                departEvenIfNotFull,
                styleKeywords,
                "같이 스시 먹으러 갈 사람 구해요.",
                "https://open.kakao.com/o/nearby123",
                LocalDateTime.of(2026, 7, 2, 14, 0)
        );
    }

    private CompanionPostDetailResult detailResult(
            final String openChatUrl,
            final CompanionPostApplyStatus applyStatus
    ) {
        return new CompanionPostDetailResult(
                101L,
                1L,
                5L,
                "google-place-id",
                LocalDateTime.of(2026, 7, 3, 14, 0),
                4,
                "같이 스시 먹으러 갈 사람 구해요.",
                openChatUrl,
                CompanionPostStatus.RECRUITING,
                LocalDateTime.of(2026, 7, 2, 13, 30),
                CompanionPostMeetingTimeType.SCHEDULED,
                null,
                2,
                applyStatus,
                new CompanionPostDetailResult.Place(
                        "google-place-id",
                        "니어바이 스시",
                        "서울시 어딘가",
                        new BigDecimal("37.56710000"),
                        new BigDecimal("126.97920000"),
                        CompanionPostPlaceCategory.RESTAURANT
                ),
                new CompanionPostDetailResult.HostProfileSummary(
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

    private Principal principal(final String name) {
        return () -> name;
    }

    private MappingJackson2HttpMessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }

    private static final class FakeReadCompanionPostsUseCase implements ReadCompanionPostsUseCase {

        private CompanionPostsResult result;
        private ReadCompanionPostsCommand command;
        private RuntimeException exception;

        @Override
        public CompanionPostsResult read(final ReadCompanionPostsCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }

    private static final class FakeCreateCompanionPostUseCase implements CreateCompanionPostUseCase {

        private CreateCompanionPostResult result;
        private CreateCompanionPostCommand command;
        private RuntimeException exception;

        @Override
        public CreateCompanionPostResult create(final CreateCompanionPostCommand command) {
            this.command = command;
            if (command == null) {
                throw new InvalidCompanionPostCreateRequestException();
            }
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }

    private static final class FakeReadCompanionPostDetailUseCase implements ReadCompanionPostDetailUseCase {

        private CompanionPostDetailResult result;
        private ReadCompanionPostDetailCommand command;
        private RuntimeException exception;

        @Override
        public CompanionPostDetailResult read(final ReadCompanionPostDetailCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }

    private static final class FakeCreateCompanionRequestUseCase implements CreateCompanionRequestUseCase {

        private CreateCompanionRequestResult result;
        private CreateCompanionRequestCommand command;
        private RuntimeException exception;

        @Override
        public CreateCompanionRequestResult create(final CreateCompanionRequestCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
