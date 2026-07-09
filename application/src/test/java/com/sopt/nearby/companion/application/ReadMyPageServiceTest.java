// 마이페이지 조회 서비스의 프로필 가공과 활동 집계를 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.application.ReadMyPageResult.AgeGroup;
import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
import com.sopt.nearby.companion.domain.model.profile.MyPageProfile;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.out.MyPageQueryPort;
import com.sopt.nearby.user.exception.OnboardingRequiredException;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadMyPageServiceTest {

    private FakeMyPageQueryPort queryPort;
    private FakeRequireCompletedOnboardingUseCase onboardingUseCase;
    private ReadMyPageService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeMyPageQueryPort();
        onboardingUseCase = new FakeRequireCompletedOnboardingUseCase();
        service = new ReadMyPageService(
                queryPort,
                onboardingUseCase,
                Clock.fixed(Instant.parse("2026-07-09T00:00:00Z"), ZoneId.of("Asia/Seoul"))
        );
    }

    @Test
    void returnsMyPage() {
        queryPort.result = Optional.of(profile(
                2003,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                List.of(
                        new MyPageProfile.CompletedMeetingPlace("강남 맛집", "서울 강남구 테헤란로"),
                        new MyPageProfile.CompletedMeetingPlace("해운대 식당", "부산 해운대구"),
                        new MyPageProfile.CompletedMeetingPlace("망원 카페", "서울 마포구")
                )
        ));

        ReadMyPageResult result = service.read(7L);

        assertEquals(7L, onboardingUseCase.userId);
        assertEquals(7L, queryPort.userId);
        assertEquals("https://cdn.nearby.com/profiles/1.jpg", result.profileImageUrl());
        assertEquals("니어바이", result.nickname());
        assertEquals(true, result.phoneVerified());
        assertEquals(AgeGroup.TWENTIES, result.ageGroup());
        assertEquals(UserGender.FEMALE, result.gender());
        assertEquals(new BigDecimal("4.00"), result.mannerScore());
        assertEquals(List.of(ReviewKeyword.FAST_RESPONSE, ReviewKeyword.PUNCTUAL), result.mannerKeywords());
        assertEquals(List.of(TravelStyleKeyword.EXTROVERTED, TravelStyleKeyword.FOODIE),
                result.travelStyleKeywords());
        assertEquals(3, result.mealTogetherCount());
        assertEquals(2, result.visitedCityCount());
        assertEquals(12, result.receivedReviewCount());
    }

    @Test
    void returnsNullAgeGroupWhenBirthYearIsMissing() {
        queryPort.result = Optional.of(profile(null, null, List.of()));

        ReadMyPageResult result = service.read(7L);

        assertEquals(false, result.phoneVerified());
        assertNull(result.ageGroup());
        assertEquals(0, result.mealTogetherCount());
        assertEquals(0, result.visitedCityCount());
    }

    @Test
    void rejectsInvalidUserIdBeforeDependencies() {
        assertThrows(
                CompanionProfileNotFoundException.class,
                () -> service.read(0L)
        );
        assertNull(onboardingUseCase.userId);
        assertNull(queryPort.userId);
    }

    @Test
    void rejectsMissingProfile() {
        queryPort.result = Optional.empty();

        assertThrows(
                CompanionProfileNotFoundException.class,
                () -> service.read(7L)
        );
    }

    @Test
    void requiresCompletedOnboarding() {
        onboardingUseCase.exception = new OnboardingRequiredException();

        assertThrows(
                OnboardingRequiredException.class,
                () -> service.read(7L)
        );
        assertNull(queryPort.userId);
    }

    private MyPageProfile profile(
            final Integer birthYear,
            final LocalDateTime phoneVerifiedAt,
            final List<MyPageProfile.CompletedMeetingPlace> completedMeetingPlaces
    ) {
        return new MyPageProfile(
                5L,
                7L,
                "니어바이",
                UserGender.FEMALE,
                birthYear,
                "https://cdn.nearby.com/profiles/1.jpg",
                new BigDecimal("4.00"),
                12,
                phoneVerifiedAt,
                List.of(TravelStyleKeyword.EXTROVERTED, TravelStyleKeyword.FOODIE),
                List.of(ReviewKeyword.FAST_RESPONSE, ReviewKeyword.PUNCTUAL),
                completedMeetingPlaces
        );
    }

    private static final class FakeMyPageQueryPort implements MyPageQueryPort {

        private Optional<MyPageProfile> result = Optional.empty();
        private Long userId;

        @Override
        public Optional<MyPageProfile> findByUserId(final Long userId) {
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
