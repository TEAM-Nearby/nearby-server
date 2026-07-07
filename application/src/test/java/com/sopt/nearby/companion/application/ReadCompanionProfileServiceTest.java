// 동행 프로필 상세 조회 서비스의 조회 기준과 예외 처리를 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileDetail;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.out.CompanionProfileDetailQueryPort;
import com.sopt.nearby.user.exception.OnboardingRequiredException;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadCompanionProfileServiceTest {

    private FakeCompanionProfileDetailQueryPort queryPort;
    private FakeRequireCompletedOnboardingUseCase onboardingUseCase;
    private ReadCompanionProfileService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeCompanionProfileDetailQueryPort();
        onboardingUseCase = new FakeRequireCompletedOnboardingUseCase();
        service = new ReadCompanionProfileService(queryPort, onboardingUseCase);
    }

    @Test
    void returnsProfileDetailByProfileId() {
        queryPort.result = Optional.of(detail(List.of(TravelStyleKeyword.PLANNED, TravelStyleKeyword.FOODIE)));

        CompanionProfileDetail result = service.read(new ReadCompanionProfileCommand(7L, 5L));

        assertEquals(7L, onboardingUseCase.userId);
        assertEquals(5L, queryPort.profileId);
        assertEquals(5L, result.profileId());
        assertEquals(1L, result.userId());
        assertEquals("니어바이", result.nickname());
        assertEquals(UserGender.FEMALE, result.gender());
        assertNull(result.birthYear());
        assertEquals("https://cdn.nearby.com/profiles/1.jpg", result.profileImageUrl());
        assertEquals("혼자 여행도 같이 여행도 좋아해요", result.intro());
        assertEquals(new BigDecimal("4.00"), result.mannerScore());
        assertEquals(12, result.reviewCount());
        assertEquals(CompanionProfileStatus.ACTIVE, result.status());
        assertEquals(LocalDateTime.of(2026, 7, 1, 10, 0), result.phoneVerifiedAt());
        assertEquals(List.of(TravelStyleKeyword.PLANNED, TravelStyleKeyword.FOODIE), result.keywords());
    }

    @Test
    void returnsEmptyKeywordsWhenProfileHasNoKeywords() {
        queryPort.result = Optional.of(detail(null));

        CompanionProfileDetail result = service.read(new ReadCompanionProfileCommand(7L, 5L));

        assertEquals(List.of(), result.keywords());
    }

    @Test
    void rejectsInvalidViewerUserIdBeforeDependencies() {
        assertThrows(
                CompanionProfileNotFoundException.class,
                () -> service.read(new ReadCompanionProfileCommand(0L, 5L))
        );
        assertNull(onboardingUseCase.userId);
        assertNull(queryPort.profileId);
    }

    @Test
    void rejectsInvalidProfileIdBeforeDependencies() {
        assertThrows(
                CompanionProfileNotFoundException.class,
                () -> service.read(new ReadCompanionProfileCommand(7L, 0L))
        );
        assertNull(onboardingUseCase.userId);
        assertNull(queryPort.profileId);
    }

    @Test
    void rejectsMissingProfile() {
        queryPort.result = Optional.empty();

        assertThrows(
                CompanionProfileNotFoundException.class,
                () -> service.read(new ReadCompanionProfileCommand(7L, 5L))
        );
    }

    @Test
    void requiresCompletedOnboarding() {
        onboardingUseCase.exception = new OnboardingRequiredException();

        assertThrows(
                OnboardingRequiredException.class,
                () -> service.read(new ReadCompanionProfileCommand(7L, 5L))
        );
    }

    private CompanionProfileDetail detail(final List<TravelStyleKeyword> keywords) {
        return new CompanionProfileDetail(
                5L,
                1L,
                "니어바이",
                UserGender.FEMALE,
                null,
                "https://cdn.nearby.com/profiles/1.jpg",
                "혼자 여행도 같이 여행도 좋아해요",
                new BigDecimal("4.00"),
                12,
                CompanionProfileStatus.ACTIVE,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                keywords
        );
    }

    private static final class FakeCompanionProfileDetailQueryPort implements CompanionProfileDetailQueryPort {

        private Optional<CompanionProfileDetail> result = Optional.empty();
        private Long profileId;

        @Override
        public Optional<CompanionProfileDetail> findByProfileId(final Long profileId) {
            this.profileId = profileId;
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
