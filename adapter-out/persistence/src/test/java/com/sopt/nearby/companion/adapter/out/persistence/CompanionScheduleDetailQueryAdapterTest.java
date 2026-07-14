// 내 동행 일정 조회 어댑터의 조인 쿼리와 매핑을 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchParticipantEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchParticipantJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleDetailJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionScheduleDetail;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionScheduleDetailQueryAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 5, 12, 0);

    @Autowired
    private CompanionPostJpaRepository companionPostJpaRepository;

    @Autowired
    private CompanionMatchJpaRepository companionMatchJpaRepository;

    @Autowired
    private CompanionMatchParticipantJpaRepository companionMatchParticipantJpaRepository;

    @Autowired
    private CompanionScheduleJpaRepository companionScheduleJpaRepository;

    @Autowired
    private CompanionScheduleDetailJpaRepository scheduleDetailJpaRepository;

    @Autowired
    private CompanionProfileJpaRepository companionProfileJpaRepository;

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Test
    void findsConfirmedScheduleDetailWithPlaceAndOpenChatUrl() {
        CompanionScheduleDetailQueryAdapter adapter = new CompanionScheduleDetailQueryAdapter(
                scheduleDetailJpaRepository
        );
        PlaceCacheEntity postPlace = placeCacheJpaRepository.saveAndFlush(place("post-place-id", "모집 장소"));
        PlaceCacheEntity schedulePlace = placeCacheJpaRepository.saveAndFlush(place(
                "schedule-place-id",
                "Siutat condal"
        ));
        CompanionPostEntity post = companionPostJpaRepository.saveAndFlush(post(
                postPlace.getId(),
                "https://open.kakao.com/o/confirmed"
        ));
        companionProfileJpaRepository.saveAndFlush(profile(7L, "루피"));
        CompanionMatchEntity match = companionMatchJpaRepository.saveAndFlush(match(
                post.getId(),
                CompanionMatchStatus.SCHEDULE_CONFIRMED
        ));
        companionMatchParticipantJpaRepository.saveAndFlush(new CompanionMatchParticipantEntity(
                null,
                match.getId(),
                7L,
                null,
                MatchParticipantRole.HOST
        ));
        CompanionScheduleEntity schedule = companionScheduleJpaRepository.saveAndFlush(schedule(
                match.getId(),
                schedulePlace.getId(),
                NOW.plusDays(2),
                true
        ));

        Optional<CompanionScheduleDetail> result = adapter.findByMatchIdAndUserId(match.getId(), 7L);

        assertThat(result).isPresent();
        CompanionScheduleDetail detail = result.get();
        assertThat(detail.matchId()).isEqualTo(match.getId());
        assertThat(detail.matchStatus()).isEqualTo(CompanionMatchStatus.SCHEDULE_CONFIRMED);
        assertThat(detail.openChatUrl()).isEqualTo("https://open.kakao.com/o/confirmed");
        assertThat(detail.schedule().scheduledAt()).isEqualTo(NOW.plusDays(2));
        assertThat(detail.schedule().place().googlePlaceId()).isEqualTo("schedule-place-id");
        assertThat(detail.schedule().place().name()).isEqualTo("Siutat condal");
        assertThat(detail.schedule().place().address()).isEqualTo("Rambla de Catalunya, 16");
        assertThat(detail.schedule().place().latitude()).isEqualByComparingTo("41.39020500");
        assertThat(detail.schedule().place().longitude()).isEqualByComparingTo("2.16354800");
        assertThat(detail.userNickname()).isEqualTo("루피");
        assertThat(detail.meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.SCHEDULED);
        assertThat(detail.currentUserRole()).isEqualTo(MatchParticipantRole.HOST);
    }

    @Test
    void returnsNullScheduleForNowMatchWhenConfirmedScheduleDoesNotExist() {
        CompanionScheduleDetailQueryAdapter adapter = new CompanionScheduleDetailQueryAdapter(
                scheduleDetailJpaRepository
        );
        PlaceCacheEntity postPlace = placeCacheJpaRepository.saveAndFlush(place("post-place-id", "모집 장소"));
        PlaceCacheEntity unconfirmedPlace = placeCacheJpaRepository.saveAndFlush(place(
                "unconfirmed-place-id",
                "미확정 장소"
        ));
        CompanionPostEntity post = companionPostJpaRepository.saveAndFlush(post(
                postPlace.getId(),
                "https://open.kakao.com/o/not-yet",
                CompanionPostMeetingTimeType.NOW
        ));
        companionProfileJpaRepository.saveAndFlush(profile(7L, "루피"));
        CompanionMatchEntity match = companionMatchJpaRepository.saveAndFlush(match(
                post.getId(),
                CompanionMatchStatus.MATCHED
        ));
        companionScheduleJpaRepository.saveAndFlush(schedule(
                match.getId(),
                unconfirmedPlace.getId(),
                NOW.plusDays(1),
                false
        ));

        Optional<CompanionScheduleDetail> result = adapter.findByMatchIdAndUserId(match.getId(), 7L);

        assertThat(result).isPresent();
        CompanionScheduleDetail detail = result.get();
        assertThat(detail.matchStatus()).isEqualTo(CompanionMatchStatus.MATCHED);
        assertThat(detail.schedule()).isNull();
        assertThat(detail.openChatUrl()).isEqualTo("https://open.kakao.com/o/not-yet");
        assertThat(detail.userNickname()).isEqualTo("루피");
        assertThat(detail.meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.NOW);
    }

    @Test
    void returnsNullScheduleAndOpenChatUrlWhenScheduledMatchHasNoConfirmedSchedule() {
        CompanionScheduleDetailQueryAdapter adapter = new CompanionScheduleDetailQueryAdapter(
                scheduleDetailJpaRepository
        );
        PlaceCacheEntity postPlace = placeCacheJpaRepository.saveAndFlush(place("post-place-id", "모집 장소"));
        CompanionPostEntity post = companionPostJpaRepository.saveAndFlush(post(
                postPlace.getId(),
                "https://open.kakao.com/o/not-yet",
                CompanionPostMeetingTimeType.SCHEDULED
        ));
        companionProfileJpaRepository.saveAndFlush(profile(7L, "루피"));
        CompanionMatchEntity match = companionMatchJpaRepository.saveAndFlush(match(
                post.getId(),
                CompanionMatchStatus.MATCHED
        ));

        Optional<CompanionScheduleDetail> result = adapter.findByMatchIdAndUserId(match.getId(), 7L);

        assertThat(result).isPresent();
        assertThat(result.get().matchStatus()).isEqualTo(CompanionMatchStatus.MATCHED);
        assertThat(result.get().schedule()).isNull();
        assertThat(result.get().openChatUrl()).isNull();
        assertThat(result.get().userNickname()).isEqualTo("루피");
        assertThat(result.get().meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.SCHEDULED);
    }

    @Test
    void findsCanceledMatchSoServiceCanReturnConflictInsteadOfNotFound() {
        CompanionScheduleDetailQueryAdapter adapter = new CompanionScheduleDetailQueryAdapter(
                scheduleDetailJpaRepository
        );
        PlaceCacheEntity postPlace = placeCacheJpaRepository.saveAndFlush(place("post-place-id", "모집 장소"));
        CompanionPostEntity post = companionPostJpaRepository.saveAndFlush(post(
                postPlace.getId(),
                "https://open.kakao.com/o/canceled"
        ));
        companionProfileJpaRepository.saveAndFlush(profile(7L, "루피"));
        CompanionMatchEntity match = companionMatchJpaRepository.saveAndFlush(match(
                post.getId(),
                CompanionMatchStatus.CANCELED
        ));

        Optional<CompanionScheduleDetail> result = adapter.findByMatchIdAndUserId(match.getId(), 7L);

        assertThat(result).isPresent();
        assertThat(result.get().matchStatus()).isEqualTo(CompanionMatchStatus.CANCELED);
    }

    @Test
    void returnsEmptyWhenMatchDoesNotExist() {
        CompanionScheduleDetailQueryAdapter adapter = new CompanionScheduleDetailQueryAdapter(
                scheduleDetailJpaRepository
        );

        Optional<CompanionScheduleDetail> result = adapter.findByMatchIdAndUserId(999L, 7L);

        assertThat(result).isEmpty();
    }

    private PlaceCacheEntity place(final String googlePlaceId, final String name) {
        return new PlaceCacheEntity(
                null,
                googlePlaceId,
                name,
                "Rambla de Catalunya, 16",
                new BigDecimal("41.39020500"),
                new BigDecimal("2.16354800"),
                "restaurant",
                null,
                new BigDecimal("4.50"),
                10,
                null,
                PlaceBusinessStatus.OPERATIONAL
        );
    }

    private CompanionPostEntity post(final Long placeId, final String openChatUrl) {
        return post(placeId, openChatUrl, CompanionPostMeetingTimeType.SCHEDULED);
    }

    private CompanionPostEntity post(
            final Long placeId,
            final String openChatUrl,
            final CompanionPostMeetingTimeType meetingTimeType
    ) {
        return new CompanionPostEntity(
                null,
                7L,
                placeId,
                meetingTimeType,
                meetingTimeType == CompanionPostMeetingTimeType.NOW ? null : NOW.plusDays(1),
                meetingTimeType == CompanionPostMeetingTimeType.NOW ? NOW.plusHours(1) : null,
                4,
                true,
                "함께 밥 먹을 동행을 구해요.",
                openChatUrl,
                CompanionPostStatus.CLOSED,
                NOW
        );
    }

    private CompanionMatchEntity match(final Long postId, final CompanionMatchStatus status) {
        return new CompanionMatchEntity(
                null,
                postId,
                status,
                NOW
        );
    }

    private CompanionScheduleEntity schedule(
            final Long matchId,
            final Long placeId,
            final LocalDateTime scheduledAt,
            final boolean confirmed
    ) {
        return new CompanionScheduleEntity(
                null,
                matchId,
                placeId,
                scheduledAt,
                120,
                confirmed
        );
    }

    private CompanionProfileEntity profile(final Long userId, final String nickname) {
        return new CompanionProfileEntity(
                null,
                userId,
                nickname,
                UserGender.FEMALE,
                1998,
                null,
                null,
                new BigDecimal("4.50"),
                0,
                CompanionProfileStatus.ACTIVE
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionPostEntity.class,
            CompanionMatchEntity.class,
            CompanionMatchParticipantEntity.class,
            CompanionScheduleEntity.class,
            CompanionProfileEntity.class,
            PlaceCacheEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionPostJpaRepository.class,
            CompanionMatchJpaRepository.class,
            CompanionMatchParticipantJpaRepository.class,
            CompanionScheduleJpaRepository.class,
            CompanionScheduleDetailJpaRepository.class,
            CompanionProfileJpaRepository.class,
            PlaceCacheJpaRepository.class
    })
    static class TestApplication {
    }
}
