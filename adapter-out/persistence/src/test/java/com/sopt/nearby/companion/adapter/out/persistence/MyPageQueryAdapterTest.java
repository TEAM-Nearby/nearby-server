// 마이페이지 조회 쿼리 어댑터의 프로필과 완료 동행 집계를 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchParticipantEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileStyleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewKeywordEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchParticipantJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileStyleJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewKeywordJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MyPageQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.MyPageProfile;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class MyPageQueryAdapterTest {

    @Autowired
    private UserAccountJpaRepository userAccountJpaRepository;

    @Autowired
    private CompanionProfileJpaRepository companionProfileJpaRepository;

    @Autowired
    private CompanionProfileStyleJpaRepository companionProfileStyleJpaRepository;

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Autowired
    private CompanionPostJpaRepository companionPostJpaRepository;

    @Autowired
    private CompanionMatchJpaRepository companionMatchJpaRepository;

    @Autowired
    private CompanionMatchParticipantJpaRepository companionMatchParticipantJpaRepository;

    @Autowired
    private CompanionScheduleJpaRepository companionScheduleJpaRepository;

    @Autowired
    private CompanionMeetingJpaRepository companionMeetingJpaRepository;

    @Autowired
    private CompanionReviewJpaRepository companionReviewJpaRepository;

    @Autowired
    private CompanionReviewKeywordJpaRepository companionReviewKeywordJpaRepository;

    @Autowired
    private MyPageQueryJpaRepository myPageQueryJpaRepository;

    @Test
    void findsMyPageProfileWithKeywordsAndCompletedMeetingPlaces() {
        MyPageQueryAdapter adapter = new MyPageQueryAdapter(myPageQueryJpaRepository);
        UserAccountEntity user = userAccountJpaRepository.saveAndFlush(user(LocalDateTime.of(2026, 7, 1, 10, 0)));
        CompanionProfileEntity profile = companionProfileJpaRepository.saveAndFlush(profile(user.getId()));
        companionProfileStyleJpaRepository.saveAndFlush(new CompanionProfileStyleEntity(
                profile.getId(),
                TravelStyleKeyword.EXTROVERTED
        ));
        companionProfileStyleJpaRepository.saveAndFlush(new CompanionProfileStyleEntity(
                profile.getId(),
                TravelStyleKeyword.FOODIE
        ));

        PlaceCacheEntity postPlace = placeCacheJpaRepository.saveAndFlush(place(
                "post-place",
                "강남 맛집",
                "서울 강남구 테헤란로"
        ));
        PlaceCacheEntity scheduledPlace = placeCacheJpaRepository.saveAndFlush(place(
                "scheduled-place",
                "해운대 식당",
                "부산 해운대구"
        ));
        CompanionMeetingEntity completedMeeting = completedMeeting(user.getId(), postPlace.getId(), scheduledPlace.getId());
        completedMeeting(user.getId(), postPlace.getId(), scheduledPlace.getId());
        ongoingMeeting(user.getId(), postPlace.getId());

        CompanionReviewEntity review = companionReviewJpaRepository.saveAndFlush(new CompanionReviewEntity(
                null,
                completedMeeting.getId(),
                99L,
                user.getId(),
                5,
                LocalDateTime.of(2026, 7, 2, 9, 0)
        ));
        companionReviewKeywordJpaRepository.saveAndFlush(new CompanionReviewKeywordEntity(
                review.getId(),
                ReviewKeyword.FAST_RESPONSE
        ));

        MyPageProfile result = adapter.findByUserId(user.getId()).orElseThrow();

        assertThat(result.profileId()).isEqualTo(profile.getId());
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.nickname()).isEqualTo("니어바이");
        assertThat(result.gender()).isEqualTo(UserGender.FEMALE);
        assertThat(result.birthYear()).isEqualTo(2003);
        assertThat(result.profileImageUrl()).isEqualTo("https://cdn.nearby.com/profiles/1.jpg");
        assertThat(result.mannerScore()).isEqualByComparingTo("4.00");
        assertThat(result.reviewCount()).isEqualTo(12);
        assertThat(result.phoneVerifiedAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 0));
        assertThat(result.travelStyleKeywords()).containsExactly(TravelStyleKeyword.EXTROVERTED, TravelStyleKeyword.FOODIE);
        assertThat(result.mannerKeywords()).containsExactly(ReviewKeyword.FAST_RESPONSE);
        assertThat(result.completedMeetingPlaces())
                .containsExactly(
                        new MyPageProfile.CompletedMeetingPlace("해운대 식당", "부산 해운대구"),
                        new MyPageProfile.CompletedMeetingPlace("해운대 식당", "부산 해운대구")
                );
    }

    @Test
    void returnsEmptyWhenProfileIsNotActive() {
        MyPageQueryAdapter adapter = new MyPageQueryAdapter(myPageQueryJpaRepository);
        UserAccountEntity user = userAccountJpaRepository.saveAndFlush(user(null));
        companionProfileJpaRepository.saveAndFlush(new CompanionProfileEntity(
                null,
                user.getId(),
                "비활성",
                UserGender.FEMALE,
                2003,
                null,
                null,
                new BigDecimal("0.00"),
                0,
                CompanionProfileStatus.INACTIVE
        ));

        assertThat(adapter.findByUserId(user.getId())).isEmpty();
    }

    private CompanionMeetingEntity completedMeeting(
            final Long userId,
            final Long postPlaceId,
            final Long scheduledPlaceId
    ) {
        CompanionPostEntity post = companionPostJpaRepository.saveAndFlush(post(userId, postPlaceId));
        CompanionMatchEntity match = companionMatchJpaRepository.saveAndFlush(match(post.getId()));
        companionMatchParticipantJpaRepository.saveAndFlush(new CompanionMatchParticipantEntity(
                null,
                match.getId(),
                userId,
                null,
                MatchParticipantRole.HOST
        ));
        companionScheduleJpaRepository.saveAndFlush(new CompanionScheduleEntity(
                null,
                match.getId(),
                scheduledPlaceId,
                LocalDateTime.of(2026, 7, 1, 19, 0),
                60,
                true
        ));
        return companionMeetingJpaRepository.saveAndFlush(new CompanionMeetingEntity(
                null,
                match.getId(),
                CompanionMeetingStatus.COMPLETED,
                LocalDateTime.of(2026, 7, 1, 19, 0),
                LocalDateTime.of(2026, 7, 1, 21, 0)
        ));
    }

    private void ongoingMeeting(final Long userId, final Long postPlaceId) {
        CompanionPostEntity post = companionPostJpaRepository.saveAndFlush(post(userId, postPlaceId));
        CompanionMatchEntity match = companionMatchJpaRepository.saveAndFlush(match(post.getId()));
        companionMatchParticipantJpaRepository.saveAndFlush(new CompanionMatchParticipantEntity(
                null,
                match.getId(),
                userId,
                null,
                MatchParticipantRole.HOST
        ));
        companionMeetingJpaRepository.saveAndFlush(new CompanionMeetingEntity(
                null,
                match.getId(),
                CompanionMeetingStatus.ONGOING,
                LocalDateTime.of(2026, 7, 2, 19, 0),
                null
        ));
    }

    private UserAccountEntity user(final LocalDateTime phoneVerifiedAt) {
        return new UserAccountEntity(
                null,
                UserRole.USER,
                UserAccountStatus.ACTIVE,
                "01012345678",
                phoneVerifiedAt,
                UserOnboardingStatus.COMPANION_PROFILE_COMPLETED,
                LocalDateTime.of(2026, 7, 1, 9, 0),
                null
        );
    }

    private CompanionProfileEntity profile(final Long userId) {
        return new CompanionProfileEntity(
                null,
                userId,
                "니어바이",
                UserGender.FEMALE,
                2003,
                "https://cdn.nearby.com/profiles/1.jpg",
                "혼자 여행도 같이 여행도 좋아해요",
                new BigDecimal("4.00"),
                12,
                CompanionProfileStatus.ACTIVE
        );
    }

    private PlaceCacheEntity place(
            final String googlePlaceId,
            final String name,
            final String address
    ) {
        return new PlaceCacheEntity(
                null,
                googlePlaceId,
                name,
                address,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                "restaurant",
                null,
                null,
                null,
                null,
                PlaceBusinessStatus.OPERATIONAL
        );
    }

    private CompanionPostEntity post(final Long hostUserId, final Long placeId) {
        return new CompanionPostEntity(
                null,
                hostUserId,
                placeId,
                LocalDateTime.of(2026, 7, 1, 19, 0),
                4,
                "같이 저녁 먹어요",
                "https://open.kakao.com/o/test",
                CompanionPostStatus.CLOSED,
                LocalDateTime.of(2026, 6, 30, 12, 0)
        );
    }

    private CompanionMatchEntity match(final Long postId) {
        return new CompanionMatchEntity(
                null,
                postId,
                CompanionMatchStatus.COMPLETED,
                LocalDateTime.of(2026, 6, 30, 13, 0)
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            UserAccountEntity.class,
            CompanionProfileEntity.class,
            CompanionProfileStyleEntity.class,
            PlaceCacheEntity.class,
            CompanionPostEntity.class,
            CompanionMatchEntity.class,
            CompanionMatchParticipantEntity.class,
            CompanionScheduleEntity.class,
            CompanionMeetingEntity.class,
            CompanionReviewEntity.class,
            CompanionReviewKeywordEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            UserAccountJpaRepository.class,
            CompanionProfileJpaRepository.class,
            CompanionProfileStyleJpaRepository.class,
            PlaceCacheJpaRepository.class,
            CompanionPostJpaRepository.class,
            CompanionMatchJpaRepository.class,
            CompanionMatchParticipantJpaRepository.class,
            CompanionScheduleJpaRepository.class,
            CompanionMeetingJpaRepository.class,
            CompanionReviewJpaRepository.class,
            CompanionReviewKeywordJpaRepository.class,
            MyPageQueryJpaRepository.class
    })
    static class TestApplication {
    }
}
