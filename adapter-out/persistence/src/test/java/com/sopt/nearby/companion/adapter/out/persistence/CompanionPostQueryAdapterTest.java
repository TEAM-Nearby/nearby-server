// 동행 모집글 목록 조회 쿼리 어댑터를 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionApplicationJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostQueryJpaRepository;
import com.sopt.nearby.companion.application.ReadCompanionPostsCommand;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostSort;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostSummary;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionPostQueryAdapterTest {

    private static final BigDecimal CURRENT_LATITUDE = new BigDecimal("37.56650000");
    private static final BigDecimal CURRENT_LONGITUDE = new BigDecimal("126.97800000");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 4, 12, 0);
    private static final LocalDateTime FUTURE = LocalDateTime.now().plusDays(3);

    @Autowired
    private CompanionPostJpaRepository companionPostJpaRepository;

    @Autowired
    private CompanionProfileJpaRepository companionProfileJpaRepository;

    @Autowired
    private CompanionApplicationJpaRepository companionApplicationJpaRepository;

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Autowired
    private CompanionPostQueryJpaRepository queryJpaRepository;

    @Test
    void findsRecruitingPostsInRadiusWithHostPlaceAndParticipantCountByLatestOrder() {
        CompanionPostQueryAdapter adapter = new CompanionPostQueryAdapter(queryJpaRepository);

        companionProfileJpaRepository.saveAndFlush(profile(100L, "호스트A", UserGender.FEMALE));
        companionProfileJpaRepository.saveAndFlush(profile(200L, "호스트B", UserGender.MALE));

        PlaceCacheEntity restaurant = placeCacheJpaRepository.saveAndFlush(place(
                "restaurant-place",
                "니어바이스시",
                "restaurant",
                "37.56660000",
                "126.97810000",
                "https://lh3.googleusercontent.com/place.jpg"
        ));
        PlaceCacheEntity cafe = placeCacheJpaRepository.saveAndFlush(place(
                "cafe-place",
                "니어바이카페",
                "cafe",
                "37.56700000",
                "126.97850000",
                null
        ));
        PlaceCacheEntity farMuseum = placeCacheJpaRepository.saveAndFlush(place(
                "far-place",
                "먼 박물관",
                "museum",
                "37.70000000",
                "126.97800000",
                null
        ));

        CompanionPostEntity oldPost = companionPostJpaRepository.saveAndFlush(post(
                100L,
                restaurant.getId(),
                CompanionPostStatus.RECRUITING,
                FUTURE.plusDays(1),
                NOW.minusHours(2),
                "오래된 모집글"
        ));
        CompanionPostEntity recentPost = companionPostJpaRepository.saveAndFlush(post(
                200L,
                cafe.getId(),
                CompanionPostStatus.RECRUITING,
                FUTURE.plusDays(2),
                NOW.minusHours(1),
                "최근 모집글"
        ));
        companionPostJpaRepository.saveAndFlush(post(
                100L,
                restaurant.getId(),
                CompanionPostStatus.CLOSED,
                FUTURE.plusDays(1),
                NOW,
                "닫힌 모집글"
        ));
        companionPostJpaRepository.saveAndFlush(post(
                100L,
                farMuseum.getId(),
                CompanionPostStatus.RECRUITING,
                FUTURE.plusDays(1),
                NOW,
                "먼 모집글"
        ));

        companionApplicationJpaRepository.saveAndFlush(application(recentPost.getId(), 301L, CompanionApplicationStatus.ACCEPTED));
        companionApplicationJpaRepository.saveAndFlush(application(recentPost.getId(), 302L, CompanionApplicationStatus.ACCEPTED));
        companionApplicationJpaRepository.saveAndFlush(application(recentPost.getId(), 303L, CompanionApplicationStatus.PENDING));
        companionApplicationJpaRepository.saveAndFlush(application(recentPost.getId(), 304L, CompanionApplicationStatus.ACCEPTED));
        companionApplicationJpaRepository.saveAndFlush(application(recentPost.getId(), 304L, CompanionApplicationStatus.CANCELED));

        List<CompanionPostSummary> result = adapter.find(command(
                1000,
                CompanionPostPlaceCategory.ALL,
                CompanionPostSort.LATEST
        ));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).postId()).isEqualTo(recentPost.getId());
        assertThat(result.get(0).hostNickname()).isEqualTo("호스트B");
        assertThat(result.get(0).hostGender()).isEqualTo(UserGender.MALE);
        assertThat(result.get(0).placeName()).isEqualTo("니어바이카페");
        assertThat(result.get(0).placeCategory()).isEqualTo(CompanionPostPlaceCategory.CAFE);
        assertThat(result.get(0).participantCount()).isEqualTo(3);
        assertThat(result.get(0).maxParticipants()).isEqualTo(4);
        assertThat(result.get(0).photoReference()).isNull();

        assertThat(result.get(1).postId()).isEqualTo(oldPost.getId());
        assertThat(result.get(1).placeCategory()).isEqualTo(CompanionPostPlaceCategory.RESTAURANT);
        assertThat(result.get(1).participantCount()).isEqualTo(1);
        assertThat(result.get(1).photoReference()).isEqualTo("https://lh3.googleusercontent.com/place.jpg");
    }

    @Test
    void filtersCategoryAndSortsByDistance() {
        CompanionPostQueryAdapter adapter = new CompanionPostQueryAdapter(queryJpaRepository);

        companionProfileJpaRepository.saveAndFlush(profile(100L, "호스트A", UserGender.FEMALE));
        PlaceCacheEntity nearRestaurant = placeCacheJpaRepository.saveAndFlush(place(
                "near-restaurant",
                "가까운 식당",
                "restaurant",
                "37.56660000",
                "126.97810000",
                null
        ));
        PlaceCacheEntity farRestaurant = placeCacheJpaRepository.saveAndFlush(place(
                "far-restaurant",
                "먼 식당",
                "restaurant",
                "37.57000000",
                "126.98200000",
                null
        ));
        PlaceCacheEntity cafe = placeCacheJpaRepository.saveAndFlush(place(
                "near-cafe",
                "가까운 카페",
                "cafe",
                "37.56655000",
                "126.97805000",
                null
        ));

        CompanionPostEntity farPost = companionPostJpaRepository.saveAndFlush(post(
                100L,
                farRestaurant.getId(),
                CompanionPostStatus.RECRUITING,
                FUTURE.plusDays(1),
                NOW,
                "먼 식당 모집글"
        ));
        CompanionPostEntity nearPost = companionPostJpaRepository.saveAndFlush(post(
                100L,
                nearRestaurant.getId(),
                CompanionPostStatus.RECRUITING,
                FUTURE.plusDays(2),
                NOW.minusHours(1),
                "가까운 식당 모집글"
        ));
        companionPostJpaRepository.saveAndFlush(post(
                100L,
                cafe.getId(),
                CompanionPostStatus.RECRUITING,
                FUTURE.plusHours(1),
                NOW.plusHours(1),
                "카페 모집글"
        ));

        List<CompanionPostSummary> result = adapter.find(command(
                1000,
                CompanionPostPlaceCategory.RESTAURANT,
                CompanionPostSort.DISTANCE
        ));

        assertThat(result).extracting(CompanionPostSummary::postId)
                .containsExactly(nearPost.getId(), farPost.getId());
        assertThat(result).allMatch(post -> post.placeCategory() == CompanionPostPlaceCategory.RESTAURANT);
    }

    @Test
    void filtersByPlaceId() {
        CompanionPostQueryAdapter adapter = new CompanionPostQueryAdapter(queryJpaRepository);

        companionProfileJpaRepository.saveAndFlush(profile(100L, "호스트A", UserGender.FEMALE));
        PlaceCacheEntity selectedPlace = placeCacheJpaRepository.saveAndFlush(place(
                "selected-restaurant",
                "선택한 식당",
                "restaurant",
                "37.56660000",
                "126.97810000",
                null
        ));
        PlaceCacheEntity otherPlace = placeCacheJpaRepository.saveAndFlush(place(
                "other-restaurant",
                "다른 식당",
                "restaurant",
                "37.56670000",
                "126.97820000",
                null
        ));

        CompanionPostEntity selectedPost = companionPostJpaRepository.saveAndFlush(post(
                100L,
                selectedPlace.getId(),
                CompanionPostStatus.RECRUITING,
                FUTURE.plusDays(1),
                NOW,
                "선택한 식당 모집글"
        ));
        companionPostJpaRepository.saveAndFlush(post(
                100L,
                otherPlace.getId(),
                CompanionPostStatus.RECRUITING,
                FUTURE.plusDays(1),
                NOW.minusMinutes(1),
                "다른 식당 모집글"
        ));

        List<CompanionPostSummary> result = adapter.find(command(
                1000,
                CompanionPostPlaceCategory.ALL,
                selectedPlace.getId(),
                CompanionPostSort.LATEST
        ));

        assertThat(result).extracting(CompanionPostSummary::postId)
                .containsExactly(selectedPost.getId());
    }

    @Test
    void sortsByClosingSoon() {
        CompanionPostQueryAdapter adapter = new CompanionPostQueryAdapter(queryJpaRepository);

        companionProfileJpaRepository.saveAndFlush(profile(100L, "호스트A", UserGender.FEMALE));
        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place(
                "restaurant-place",
                "식당",
                "restaurant",
                "37.56660000",
                "126.97810000",
                null
        ));

        CompanionPostEntity latePost = companionPostJpaRepository.saveAndFlush(post(
                100L,
                place.getId(),
                CompanionPostStatus.RECRUITING,
                FUTURE.plusDays(2),
                NOW,
                "늦은 모집글"
        ));
        CompanionPostEntity soonPost = companionPostJpaRepository.saveAndFlush(post(
                100L,
                place.getId(),
                CompanionPostStatus.RECRUITING,
                FUTURE.plusHours(2),
                NOW.minusHours(1),
                "임박 모집글"
        ));
        CompanionPostEntity nowPost = companionPostJpaRepository.saveAndFlush(post(
                100L,
                place.getId(),
                CompanionPostStatus.RECRUITING,
                CompanionPostMeetingTimeType.NOW,
                null,
                FUTURE.minusDays(2),
                NOW.minusMinutes(10),
                "지금 모집글"
        ));

        List<CompanionPostSummary> result = adapter.find(command(
                1000,
                CompanionPostPlaceCategory.ALL,
                CompanionPostSort.CLOSING_SOON
        ));

        assertThat(result).extracting(CompanionPostSummary::postId)
                .containsExactly(nowPost.getId(), soonPost.getId(), latePost.getId());
    }

    @Test
    void excludesExpiredNowPosts() {
        CompanionPostQueryAdapter adapter = new CompanionPostQueryAdapter(queryJpaRepository);

        companionProfileJpaRepository.saveAndFlush(profile(100L, "호스트A", UserGender.FEMALE));
        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place(
                "restaurant-place",
                "식당",
                "restaurant",
                "37.56660000",
                "126.97810000",
                null
        ));

        companionPostJpaRepository.saveAndFlush(post(
                100L,
                place.getId(),
                CompanionPostStatus.RECRUITING,
                CompanionPostMeetingTimeType.NOW,
                null,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusHours(2),
                "만료된 지금 모집글"
        ));
        CompanionPostEntity activePost = companionPostJpaRepository.saveAndFlush(post(
                100L,
                place.getId(),
                CompanionPostStatus.RECRUITING,
                CompanionPostMeetingTimeType.NOW,
                null,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().minusMinutes(10),
                "노출 중인 지금 모집글"
        ));

        List<CompanionPostSummary> result = adapter.find(command(
                1000,
                CompanionPostPlaceCategory.ALL,
                CompanionPostSort.LATEST
        ));

        assertThat(result).extracting(CompanionPostSummary::postId)
                .containsExactly(activePost.getId());
        assertThat(result.get(0).meetingAt()).isNull();
    }

    @Test
    void excludesScheduledPostsAfterMeetingAt() {
        CompanionPostQueryAdapter adapter = new CompanionPostQueryAdapter(queryJpaRepository);

        companionProfileJpaRepository.saveAndFlush(profile(100L, "호스트A", UserGender.FEMALE));
        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place(
                "restaurant-place",
                "식당",
                "restaurant",
                "37.56660000",
                "126.97810000",
                null
        ));

        companionPostJpaRepository.saveAndFlush(post(
                100L,
                place.getId(),
                CompanionPostStatus.RECRUITING,
                CompanionPostMeetingTimeType.SCHEDULED,
                LocalDateTime.now().minusHours(1),
                null,
                LocalDateTime.now().minusDays(1),
                "지난 예약 모집글"
        ));
        CompanionPostEntity activePost = companionPostJpaRepository.saveAndFlush(post(
                100L,
                place.getId(),
                CompanionPostStatus.RECRUITING,
                CompanionPostMeetingTimeType.SCHEDULED,
                LocalDateTime.now().plusHours(1),
                null,
                LocalDateTime.now().minusMinutes(10),
                "예정된 예약 모집글"
        ));

        List<CompanionPostSummary> result = adapter.find(command(
                1000,
                CompanionPostPlaceCategory.ALL,
                CompanionPostSort.LATEST
        ));

        assertThat(result).extracting(CompanionPostSummary::postId)
                .containsExactly(activePost.getId());
    }

    private ReadCompanionPostsCommand command(
            final int radiusMeters,
            final CompanionPostPlaceCategory placeCategory,
            final CompanionPostSort sort
    ) {
        return command(radiusMeters, placeCategory, null, sort);
    }

    private ReadCompanionPostsCommand command(
            final int radiusMeters,
            final CompanionPostPlaceCategory placeCategory,
            final Long placeId,
            final CompanionPostSort sort
    ) {
        return new ReadCompanionPostsCommand(
                7L,
                CURRENT_LATITUDE,
                CURRENT_LONGITUDE,
                radiusMeters,
                placeCategory,
                placeId,
                sort
        );
    }

    private PlaceCacheEntity place(
            final String googlePlaceId,
            final String name,
            final String category,
            final String latitude,
            final String longitude,
            final String photoReference
    ) {
        return new PlaceCacheEntity(
                null,
                googlePlaceId,
                name,
                "서울시 어딘가",
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                category,
                null,
                new BigDecimal("4.50"),
                10,
                photoReference,
                PlaceBusinessStatus.OPERATIONAL
        );
    }

    private CompanionProfileEntity profile(final Long userId, final String nickname, final UserGender gender) {
        return new CompanionProfileEntity(
                null,
                userId,
                nickname,
                gender,
                2000,
                null,
                "반가워요.",
                new BigDecimal("4.50"),
                0,
                CompanionProfileStatus.ACTIVE
        );
    }

    private CompanionPostEntity post(
            final Long hostUserId,
            final Long placeId,
            final CompanionPostStatus status,
            final LocalDateTime meetingAt,
            final LocalDateTime createdAt,
            final String content
    ) {
        return new CompanionPostEntity(
                null,
                hostUserId,
                placeId,
                meetingAt,
                4,
                content,
                "https://openchat.example",
                status,
                createdAt
        );
    }

    private CompanionPostEntity post(
            final Long hostUserId,
            final Long placeId,
            final CompanionPostStatus status,
            final CompanionPostMeetingTimeType meetingTimeType,
            final LocalDateTime meetingAt,
            final LocalDateTime exposureExpiresAt,
            final LocalDateTime createdAt,
            final String content
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
                content,
                "https://openchat.example",
                status,
                createdAt
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
                NOW
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionApplicationEntity.class,
            CompanionPostEntity.class,
            CompanionProfileEntity.class,
            PlaceCacheEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionApplicationJpaRepository.class,
            CompanionPostJpaRepository.class,
            CompanionProfileJpaRepository.class,
            CompanionPostQueryJpaRepository.class,
            PlaceCacheJpaRepository.class
    })
    static class TestApplication {
    }
}
