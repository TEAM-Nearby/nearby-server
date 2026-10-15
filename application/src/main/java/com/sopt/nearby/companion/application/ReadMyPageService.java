// 마이페이지 조회 유스케이스를 구현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.application.ReadMyPageResult.AgeGroup;
import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.domain.model.profile.MyPageProfile;
import com.sopt.nearby.companion.port.in.ReadMyPageUseCase;
import com.sopt.nearby.companion.port.out.MyPageQueryPort;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import com.sopt.nearby.user.port.in.ReadOnboardingStatusUseCase;
import java.util.List;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.transaction.annotation.Transactional;

public class ReadMyPageService implements ReadMyPageUseCase {

    private final MyPageQueryPort queryPort;
    private final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase;
    private final Clock clock;
    private final ReadOnboardingStatusUseCase readOnboardingStatus;

    public ReadMyPageService(
            final MyPageQueryPort queryPort,
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase,
            final ReadOnboardingStatusUseCase readOnboardingStatus,
            final Clock clock
    ) {
        this.queryPort = queryPort;
        this.requireCompletedOnboardingUseCase = requireCompletedOnboardingUseCase;
        this.clock = clock;
        this.readOnboardingStatus = readOnboardingStatus;
    }

    @Override
    @Transactional(readOnly = true)
    public ReadMyPageResult read(final Long userId) {
        if (userId == null || userId <= 0) {
            throw new CompanionProfileNotFoundException();
        }

        requireCompletedOnboardingUseCase.requireCompleted(userId);
        ReadOnboardingStatusUseCase.Status onboarding = readOnboardingStatus.read(userId);
        if (!onboarding.hasCompanionProfile()) {
            return new ReadMyPageResult(null, null, onboarding.phoneVerified(), null, null, null,
                    List.of(), List.of(), 0, 0, 0, false);
        }
        MyPageProfile profile = queryPort.findByUserId(userId)
                .orElseThrow(CompanionProfileNotFoundException::new);

        return new ReadMyPageResult(
                profile.profileImageUrl(),
                profile.nickname(),
                profile.phoneVerifiedAt() != null,
                ageGroup(profile.birthYear()),
                profile.gender(),
                profile.mannerScore(),
                profile.mannerKeywords(),
                profile.travelStyleKeywords(),
                profile.completedMeetingPlaces().size(),
                visitedCityCount(profile),
                profile.reviewCount(),
                true
        );
    }

    private AgeGroup ageGroup(final Integer birthYear) {
        if (birthYear == null) {
            return null;
        }

        int age = LocalDate.now(clock).getYear() - birthYear;
        if (age < 0) {
            return null;
        }
        if (age < 20) {
            return AgeGroup.TEENS;
        }
        if (age < 30) {
            return AgeGroup.TWENTIES;
        }
        if (age < 40) {
            return AgeGroup.THIRTIES;
        }
        if (age < 50) {
            return AgeGroup.FORTIES;
        }
        if (age < 60) {
            return AgeGroup.FIFTIES;
        }
        return AgeGroup.SIXTIES_OR_ABOVE;
    }

    private int visitedCityCount(final MyPageProfile profile) {
        return (int) profile.completedMeetingPlaces()
                .stream()
                .map(place -> CompanionPlaceCityNameResolver.resolve(place.address()))
                .filter(cityName -> !cityName.isBlank())
                .distinct()
                .count();
    }
}
