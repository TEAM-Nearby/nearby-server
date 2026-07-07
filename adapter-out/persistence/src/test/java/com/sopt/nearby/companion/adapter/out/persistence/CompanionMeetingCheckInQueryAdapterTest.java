// 만남 인증 조회 어댑터의 만남, 일정, 장소 조인과 매핑을 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingCheckInQueryJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingCheckInContext;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
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
class CompanionMeetingCheckInQueryAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 18, 30);

    @Autowired
    private CompanionPostJpaRepository postJpaRepository;

    @Autowired
    private CompanionMatchJpaRepository matchJpaRepository;

    @Autowired
    private CompanionMeetingJpaRepository meetingJpaRepository;

    @Autowired
    private CompanionScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private CompanionMeetingCheckInQueryJpaRepository queryJpaRepository;

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Test
    void findsMeetingCheckInContextWithConfirmedScheduleAndPlace() {
        CompanionMeetingCheckInQueryAdapter adapter = new CompanionMeetingCheckInQueryAdapter(queryJpaRepository);
        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place());
        CompanionPostEntity post = postJpaRepository.saveAndFlush(post(place.getId()));
        CompanionMatchEntity match = matchJpaRepository.saveAndFlush(match(post.getId()));
        CompanionMeetingEntity meeting = meetingJpaRepository.saveAndFlush(meeting(match.getId()));
        CompanionScheduleEntity schedule = scheduleJpaRepository.saveAndFlush(schedule(match.getId(), place.getId()));

        Optional<CompanionMeetingCheckInContext> result = adapter.findByMeetingId(meeting.getId());

        assertThat(result).isPresent();
        CompanionMeetingCheckInContext context = result.get();
        assertThat(context.meetingId()).isEqualTo(meeting.getId());
        assertThat(context.matchId()).isEqualTo(match.getId());
        assertThat(context.meetingStatus()).isEqualTo(CompanionMeetingStatus.ONGOING);
        assertThat(context.scheduleId()).isEqualTo(schedule.getId());
        assertThat(context.placeId()).isEqualTo(place.getId());
        assertThat(context.scheduledAt()).isEqualTo(NOW);
        assertThat(context.placeLatitude()).isEqualByComparingTo("41.39020500");
        assertThat(context.placeLongitude()).isEqualByComparingTo("2.16354800");
        assertThat(context.hasConfirmedSchedulePlace()).isTrue();
    }

    @Test
    void returnsMeetingContextWithoutScheduleWhenScheduleIsNotConfirmed() {
        CompanionMeetingCheckInQueryAdapter adapter = new CompanionMeetingCheckInQueryAdapter(queryJpaRepository);
        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place());
        CompanionPostEntity post = postJpaRepository.saveAndFlush(post(place.getId()));
        CompanionMatchEntity match = matchJpaRepository.saveAndFlush(match(post.getId()));
        CompanionMeetingEntity meeting = meetingJpaRepository.saveAndFlush(meeting(match.getId()));

        Optional<CompanionMeetingCheckInContext> result = adapter.findByMeetingId(meeting.getId());

        assertThat(result).isPresent();
        assertThat(result.get().meetingId()).isEqualTo(meeting.getId());
        assertThat(result.get().hasConfirmedSchedulePlace()).isFalse();
    }

    @Test
    void returnsEmptyWhenMeetingDoesNotExist() {
        CompanionMeetingCheckInQueryAdapter adapter = new CompanionMeetingCheckInQueryAdapter(queryJpaRepository);

        Optional<CompanionMeetingCheckInContext> result = adapter.findByMeetingId(999L);

        assertThat(result).isEmpty();
    }

    private PlaceCacheEntity place() {
        return new PlaceCacheEntity(
                null,
                "google-place-id",
                "Siutat condal",
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

    private CompanionPostEntity post(final Long placeId) {
        return new CompanionPostEntity(
                null,
                7L,
                placeId,
                NOW,
                4,
                "함께 밥 먹을 동행을 구해요.",
                "https://openchat.example",
                CompanionPostStatus.CLOSED,
                NOW.minusDays(1)
        );
    }

    private CompanionMatchEntity match(final Long postId) {
        return new CompanionMatchEntity(null, postId, CompanionMatchStatus.SCHEDULE_CONFIRMED, NOW.minusDays(1));
    }

    private CompanionMeetingEntity meeting(final Long matchId) {
        return new CompanionMeetingEntity(null, matchId, CompanionMeetingStatus.ONGOING, NOW.minusMinutes(5), null);
    }

    private CompanionScheduleEntity schedule(final Long matchId, final Long placeId) {
        return new CompanionScheduleEntity(null, matchId, placeId, NOW, 120, true);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionPostEntity.class,
            CompanionMatchEntity.class,
            CompanionMeetingEntity.class,
            CompanionScheduleEntity.class,
            PlaceCacheEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionPostJpaRepository.class,
            CompanionMatchJpaRepository.class,
            CompanionMeetingJpaRepository.class,
            CompanionScheduleJpaRepository.class,
            CompanionMeetingCheckInQueryJpaRepository.class,
            PlaceCacheJpaRepository.class
    })
    static class TestApplication {
    }
}
