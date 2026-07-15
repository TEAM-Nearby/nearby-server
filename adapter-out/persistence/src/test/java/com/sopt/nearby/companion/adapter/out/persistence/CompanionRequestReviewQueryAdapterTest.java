// 동행 신청 상세 조회 쿼리 어댑터의 조인 결과를 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionApplicationJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionRequestReviewQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionRequestReview;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.user.adapter.out.persistence.entity.UserAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.repository.UserAccountJpaRepository;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionRequestReviewQueryAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 2, 12, 0);

    @Autowired
    private CompanionApplicationJpaRepository applicationJpaRepository;

    @Autowired
    private CompanionPostJpaRepository postJpaRepository;

    @Autowired
    private CompanionProfileJpaRepository profileJpaRepository;

    @Autowired
    private CompanionRequestReviewQueryJpaRepository queryJpaRepository;

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Autowired
    private UserAccountJpaRepository userAccountJpaRepository;

    @Test
    void findsCompanionRequestReviewWithApplicantProfileAndAccount() {
        CompanionRequestReviewQueryAdapter adapter = new CompanionRequestReviewQueryAdapter(queryJpaRepository);

        UserAccountEntity hostUser = userAccountJpaRepository.saveAndFlush(user(null));
        UserAccountEntity applicantUser = userAccountJpaRepository.saveAndFlush(user(
                LocalDateTime.of(2026, 7, 1, 9, 0)
        ));
        profileJpaRepository.saveAndFlush(profile(hostUser.getId(), "호스트", UserGender.MALE, null));
        CompanionProfileEntity applicantProfile = profileJpaRepository.saveAndFlush(profile(
                applicantUser.getId(),
                "지민",
                UserGender.FEMALE,
                "https://cdn.nearby/profile/2.png"
        ));

        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place());
        CompanionPostEntity post = postJpaRepository.saveAndFlush(post(
                hostUser.getId(),
                place.getId(),
                CompanionPostMeetingTimeType.SCHEDULED,
                LocalDateTime.of(2026, 6, 18, 16, 30),
                null
        ));
        CompanionApplicationEntity application = applicationJpaRepository.saveAndFlush(application(
                post.getId(),
                applicantUser.getId(),
                CompanionApplicationStatus.PENDING
        ));

        CompanionRequestReview result = adapter.findByApplicationId(application.getId()).orElseThrow();

        assertThat(result.applicationId()).isEqualTo(application.getId());
        assertThat(result.postId()).isEqualTo(post.getId());
        assertThat(result.applicationStatus()).isEqualTo(CompanionApplicationStatus.PENDING);
        assertThat(result.hostUserId()).isEqualTo(hostUser.getId());
        assertThat(result.placeName()).isEqualTo("오노테라");
        assertThat(result.meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.SCHEDULED);
        assertThat(result.meetingAt()).isEqualTo(LocalDateTime.of(2026, 6, 18, 16, 30));
        assertThat(result.exposureExpiresAt()).isNull();
        assertThat(result.applicantProfile().profileId()).isEqualTo(applicantProfile.getId());
        assertThat(result.applicantProfile().profileImageUrl())
                .isEqualTo("https://cdn.nearby/profile/2.png");
        assertThat(result.applicantProfile().nickname()).isEqualTo("지민");
        assertThat(result.applicantProfile().gender()).isEqualTo(UserGender.FEMALE);
        assertThat(result.applicantProfile().birthYear()).isEqualTo(2003);
        assertThat(result.applicantProfile().mannerScore()).isEqualByComparingTo("4.00");
        assertThat(result.applicantAccount().phoneVerifiedAt())
                .isEqualTo(LocalDateTime.of(2026, 7, 1, 9, 0));
    }

    @Test
    void readsExposureExpiresAtForNowPost() {
        CompanionRequestReviewQueryAdapter adapter = new CompanionRequestReviewQueryAdapter(queryJpaRepository);

        UserAccountEntity hostUser = userAccountJpaRepository.saveAndFlush(user(null));
        UserAccountEntity applicantUser = userAccountJpaRepository.saveAndFlush(user(null));
        profileJpaRepository.saveAndFlush(profile(hostUser.getId(), "호스트", UserGender.MALE, null));
        profileJpaRepository.saveAndFlush(profile(applicantUser.getId(), "지민", UserGender.FEMALE, null));
        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place());
        CompanionPostEntity post = postJpaRepository.saveAndFlush(post(
                hostUser.getId(),
                place.getId(),
                CompanionPostMeetingTimeType.NOW,
                null,
                LocalDateTime.of(2026, 6, 18, 17, 30)
        ));
        CompanionApplicationEntity application = applicationJpaRepository.saveAndFlush(application(
                post.getId(),
                applicantUser.getId(),
                CompanionApplicationStatus.PENDING
        ));

        CompanionRequestReview result = adapter.findByApplicationId(application.getId()).orElseThrow();

        assertThat(result.meetingAt()).isNull();
        assertThat(result.exposureExpiresAt()).isEqualTo(LocalDateTime.of(2026, 6, 18, 17, 30));
    }

    @Test
    void returnsEmptyWhenApplicationDoesNotExist() {
        CompanionRequestReviewQueryAdapter adapter = new CompanionRequestReviewQueryAdapter(queryJpaRepository);

        assertThat(adapter.findByApplicationId(999L)).isEmpty();
    }

    private UserAccountEntity user(final LocalDateTime phoneVerifiedAt) {
        return new UserAccountEntity(
                null,
                UserRole.USER,
                UserAccountStatus.ACTIVE,
                "010-1234-5678",
                phoneVerifiedAt,
                UserOnboardingStatus.COMPLETED,
                NOW.minusDays(1),
                null
        );
    }

    private CompanionProfileEntity profile(
            final Long userId,
            final String nickname,
            final UserGender gender,
            final String profileImageUrl
    ) {
        return new CompanionProfileEntity(
                null,
                userId,
                nickname,
                gender,
                2003,
                profileImageUrl,
                "반가워요.",
                new BigDecimal("4.00"),
                3,
                CompanionProfileStatus.ACTIVE
        );
    }

    private PlaceCacheEntity place() {
        return new PlaceCacheEntity(
                null,
                "google-place-id",
                "오노테라",
                "서울시 어딘가",
                new BigDecimal("37.56710000"),
                new BigDecimal("126.97920000"),
                "restaurant",
                null,
                new BigDecimal("4.50"),
                10,
                null,
                PlaceBusinessStatus.OPERATIONAL
        );
    }

    private CompanionPostEntity post(
            final Long hostUserId,
            final Long placeId,
            final CompanionPostMeetingTimeType meetingTimeType,
            final LocalDateTime meetingAt,
            final LocalDateTime exposureExpiresAt
    ) {
        return new CompanionPostEntity(
                null,
                hostUserId,
                placeId,
                meetingTimeType,
                meetingAt,
                exposureExpiresAt,
                4,
                true,
                "같이 밥 먹을 사람 구해요.",
                "https://open.kakao.com/o/nearby123",
                CompanionPostStatus.RECRUITING,
                NOW.minusHours(1)
        );
    }

    private CompanionApplicationEntity application(
            final Long postId,
            final Long applicantUserId,
            final CompanionApplicationStatus status
    ) {
        return new CompanionApplicationEntity(
                null,
                postId,
                applicantUserId,
                status,
                null,
                NOW.minusMinutes(30)
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionApplicationEntity.class,
            CompanionPostEntity.class,
            CompanionProfileEntity.class,
            PlaceCacheEntity.class,
            UserAccountEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionApplicationJpaRepository.class,
            CompanionPostJpaRepository.class,
            CompanionProfileJpaRepository.class,
            CompanionRequestReviewQueryJpaRepository.class,
            PlaceCacheJpaRepository.class,
            UserAccountJpaRepository.class
    })
    static class TestApplication {
    }
}
