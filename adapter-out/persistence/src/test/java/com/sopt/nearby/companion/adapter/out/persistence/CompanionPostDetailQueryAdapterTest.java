// 동행 모집 글 상세 조회 쿼리 어댑터의 조인 결과를 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileStyleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewKeywordEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionApplicationJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostDetailQueryJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostQueryJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostDetail;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionPostDetailQueryAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 2, 12, 0);

    @Autowired
    private CompanionApplicationJpaRepository applicationJpaRepository;

    @Autowired
    private CompanionPostJpaRepository postJpaRepository;

    @Autowired
    private CompanionPostDetailQueryJpaRepository queryJpaRepository;

    @Autowired
    private CompanionPostQueryJpaRepository postQueryJpaRepository;

    @Autowired
    private CompanionProfileJpaRepository profileJpaRepository;

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Autowired
    private UserAccountJpaRepository userAccountJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findsPostDetailWithHostPlaceParticipantCountLatestApplyStatusAndKeywords() {
        CompanionPostDetailQueryAdapter adapter = new CompanionPostDetailQueryAdapter(
                queryJpaRepository,
                postQueryJpaRepository
        );

        UserAccountEntity hostUser = userAccountJpaRepository.saveAndFlush(user(
                LocalDateTime.of(2026, 7, 1, 10, 0)
        ));
        CompanionProfileEntity hostProfile = profileJpaRepository.saveAndFlush(profile(
                hostUser.getId(),
                "니어바이",
                UserGender.FEMALE,
                "https://cdn.nearby.com/profiles/1.jpg"
        ));
        entityManager.persistAndFlush(new CompanionProfileStyleEntity(hostProfile.getId(), TravelStyleKeyword.PLANNED));
        entityManager.persistAndFlush(new CompanionProfileStyleEntity(hostProfile.getId(), TravelStyleKeyword.FOODIE));

        UserAccountEntity acceptedUser = userAccountJpaRepository.saveAndFlush(user(null));
        profileJpaRepository.saveAndFlush(profile(
                acceptedUser.getId(),
                "확정참여자",
                UserGender.MALE,
                "https://cdn.nearby.com/profiles/2.jpg"
        ));
        UserAccountEntity acceptedWithoutImageUser = userAccountJpaRepository.saveAndFlush(user(null));
        profileJpaRepository.saveAndFlush(profile(
                acceptedWithoutImageUser.getId(),
                "이미지없는참여자",
                UserGender.FEMALE,
                null
        ));

        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place());
        CompanionPostEntity post = postJpaRepository.saveAndFlush(post(
                hostUser.getId(),
                place.getId(),
                CompanionPostMeetingTimeType.SCHEDULED,
                NOW.plusHours(2),
                null
        ));
        CompanionMatchEntity match = entityManager.persistAndFlush(new CompanionMatchEntity(
                null,
                post.getId(),
                CompanionMatchStatus.MATCHED,
                NOW
        ));
        CompanionMeetingEntity meeting = entityManager.persistAndFlush(new CompanionMeetingEntity(
                null,
                match.getId(),
                CompanionMeetingStatus.COMPLETED,
                NOW,
                NOW.plusHours(1)
        ));
        CompanionReviewEntity review = entityManager.persistAndFlush(new CompanionReviewEntity(
                null,
                meeting.getId(),
                acceptedUser.getId(),
                hostUser.getId(),
                5,
                NOW
        ));
        entityManager.persistAndFlush(new CompanionReviewKeywordEntity(
                review.getId(),
                ReviewKeyword.FAST_RESPONSE
        ));

        applicationJpaRepository.saveAndFlush(application(
                post.getId(),
                acceptedUser.getId(),
                CompanionApplicationStatus.ACCEPTED,
                NOW.minusHours(1)
        ));
        applicationJpaRepository.saveAndFlush(application(
                post.getId(),
                acceptedWithoutImageUser.getId(),
                CompanionApplicationStatus.ACCEPTED,
                NOW.minusMinutes(30)
        ));
        applicationJpaRepository.saveAndFlush(application(
                post.getId(),
                9L,
                CompanionApplicationStatus.REJECTED,
                NOW.minusMinutes(20)
        ));
        applicationJpaRepository.saveAndFlush(application(
                post.getId(),
                10L,
                CompanionApplicationStatus.CANCELED,
                NOW.minusMinutes(10)
        ));
        applicationJpaRepository.saveAndFlush(application(
                post.getId(),
                11L,
                CompanionApplicationStatus.PENDING,
                NOW.minusMinutes(5)
        ));

        CompanionPostDetail result = adapter.findByPostId(post.getId(), acceptedUser.getId()).orElseThrow();

        assertThat(result.postId()).isEqualTo(post.getId());
        assertThat(result.hostUserId()).isEqualTo(hostUser.getId());
        assertThat(result.meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.SCHEDULED);
        assertThat(result.meetingAt()).isEqualTo(NOW.plusHours(2));
        assertThat(result.expiresAt()).isNull();
        assertThat(result.participantCount()).isEqualTo(3);
        assertThat(result.applicationStatus()).isEqualTo(CompanionApplicationStatus.ACCEPTED);
        assertThat(result.place().googlePlaceId()).isEqualTo("google-place-id");
        assertThat(result.place().name()).isEqualTo("니어바이 스시");
        assertThat(result.place().category()).isEqualTo(CompanionPostPlaceCategory.RESTAURANT);
        assertThat(result.hostProfileSummary().profileId()).isEqualTo(hostProfile.getId());
        assertThat(result.hostProfileSummary().nickname()).isEqualTo("니어바이");
        assertThat(result.hostProfileSummary().intro()).isEqualTo("반가워요.");
        assertThat(result.hostProfileSummary().mannerScore()).isEqualByComparingTo("5.00");
        assertThat(result.hostProfileSummary().mannerKeywords()).containsExactly(ReviewKeyword.FAST_RESPONSE);
        assertThat(result.hostProfileSummary().phoneVerifiedAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 0));
        assertThat(result.hostProfileSummary().keywords())
                .containsExactlyInAnyOrder(TravelStyleKeyword.PLANNED, TravelStyleKeyword.FOODIE);
        assertThat(result.participants())
                .extracting(
                        CompanionPostDetail.Participant::userId,
                        CompanionPostDetail.Participant::profileImageUrl
                )
                .containsExactly(
                        tuple(hostUser.getId(), "https://cdn.nearby.com/profiles/1.jpg"),
                        tuple(acceptedUser.getId(), "https://cdn.nearby.com/profiles/2.jpg"),
                        tuple(acceptedWithoutImageUser.getId(), null)
                );
    }

    @Test
    void returnsNullApplyStatusAndEmptyKeywordsWhenUserHasNotApplied() {
        CompanionPostDetailQueryAdapter adapter = new CompanionPostDetailQueryAdapter(
                queryJpaRepository,
                postQueryJpaRepository
        );

        UserAccountEntity hostUser = userAccountJpaRepository.saveAndFlush(user(null));
        CompanionProfileEntity hostProfile = profileJpaRepository.saveAndFlush(profile(
                hostUser.getId(),
                "니어바이",
                UserGender.MALE,
                null
        ));
        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place());
        CompanionPostEntity post = postJpaRepository.saveAndFlush(post(
                hostUser.getId(),
                place.getId(),
                CompanionPostMeetingTimeType.NOW,
                null,
                NOW.plusHours(1)
        ));

        CompanionPostDetail result = adapter.findByPostId(post.getId(), 7L).orElseThrow();

        assertThat(result.applicationStatus()).isNull();
        assertThat(result.participantCount()).isEqualTo(1);
        assertThat(result.expiresAt()).isEqualTo(NOW.plusHours(1));
        assertThat(result.hostProfileSummary().profileId()).isEqualTo(hostProfile.getId());
        assertThat(result.hostProfileSummary().profileImageUrl()).isNull();
        assertThat(result.hostProfileSummary().phoneVerifiedAt()).isNull();
        assertThat(result.hostProfileSummary().keywords()).isEmpty();
    }

    @Test
    void returnsEmptyWhenPostDoesNotExist() {
        CompanionPostDetailQueryAdapter adapter = new CompanionPostDetailQueryAdapter(
                queryJpaRepository,
                postQueryJpaRepository
        );

        assertThat(adapter.findByPostId(999L, 7L)).isEmpty();
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
                2001,
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
                "니어바이 스시",
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
                "같이 스시 먹으러 갈 사람 구해요.",
                "https://open.kakao.com/o/nearby123",
                CompanionPostStatus.RECRUITING,
                NOW.minusHours(1)
        );
    }

    private CompanionApplicationEntity application(
            final Long postId,
            final Long applicantUserId,
            final CompanionApplicationStatus status,
            final LocalDateTime createdAt
    ) {
        return new CompanionApplicationEntity(
                null,
                postId,
                applicantUserId,
                status,
                null,
                createdAt
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionApplicationEntity.class,
            CompanionMatchEntity.class,
            CompanionMeetingEntity.class,
            CompanionPostEntity.class,
            CompanionProfileEntity.class,
            CompanionProfileStyleEntity.class,
            CompanionReviewEntity.class,
            CompanionReviewKeywordEntity.class,
            PlaceCacheEntity.class,
            UserAccountEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionApplicationJpaRepository.class,
            CompanionPostDetailQueryJpaRepository.class,
            CompanionPostJpaRepository.class,
            CompanionProfileJpaRepository.class,
            PlaceCacheJpaRepository.class,
            UserAccountJpaRepository.class
    })
    static class TestApplication {
    }
}
