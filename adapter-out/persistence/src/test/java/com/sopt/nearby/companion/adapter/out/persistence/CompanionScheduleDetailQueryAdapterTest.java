// 내 동행 일정 조회 어댑터의 조인 쿼리와 매핑을 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleDetailJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionScheduleDetail;
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
class CompanionScheduleDetailQueryAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 5, 12, 0);

    @Autowired
    private CompanionPostJpaRepository companionPostJpaRepository;

    @Autowired
    private CompanionMatchJpaRepository companionMatchJpaRepository;

    @Autowired
    private CompanionScheduleJpaRepository companionScheduleJpaRepository;

    @Autowired
    private CompanionScheduleDetailJpaRepository scheduleDetailJpaRepository;

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
        CompanionMatchEntity match = companionMatchJpaRepository.saveAndFlush(match(
                post.getId(),
                CompanionMatchStatus.SCHEDULE_CONFIRMED
        ));
        CompanionScheduleEntity schedule = companionScheduleJpaRepository.saveAndFlush(schedule(
                match.getId(),
                schedulePlace.getId(),
                NOW.plusDays(2),
                true
        ));

        Optional<CompanionScheduleDetail> result = adapter.findByMatchId(match.getId());

        assertThat(result).isPresent();
        CompanionScheduleDetail detail = result.get();
        assertThat(detail.matchId()).isEqualTo(match.getId());
        assertThat(detail.matchStatus()).isEqualTo(CompanionMatchStatus.SCHEDULE_CONFIRMED);
        assertThat(detail.openChatUrl()).isEqualTo("https://open.kakao.com/o/confirmed");
        assertThat(detail.schedule().scheduleId()).isEqualTo(schedule.getId());
        assertThat(detail.schedule().scheduledAt()).isEqualTo(NOW.plusDays(2));
        assertThat(detail.schedule().place().googlePlaceId()).isEqualTo("schedule-place-id");
        assertThat(detail.schedule().place().name()).isEqualTo("Siutat condal");
        assertThat(detail.schedule().place().address()).isEqualTo("Rambla de Catalunya, 16");
        assertThat(detail.schedule().place().latitude()).isEqualByComparingTo("41.39020500");
        assertThat(detail.schedule().place().longitude()).isEqualByComparingTo("2.16354800");
    }

    @Test
    void returnsNullScheduleAndOpenChatUrlWhenConfirmedScheduleDoesNotExist() {
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
                "https://open.kakao.com/o/not-yet"
        ));
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

        Optional<CompanionScheduleDetail> result = adapter.findByMatchId(match.getId());

        assertThat(result).isPresent();
        assertThat(result.get().matchStatus()).isEqualTo(CompanionMatchStatus.MATCHED);
        assertThat(result.get().schedule()).isNull();
        assertThat(result.get().openChatUrl()).isNull();
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
        CompanionMatchEntity match = companionMatchJpaRepository.saveAndFlush(match(
                post.getId(),
                CompanionMatchStatus.CANCELED
        ));

        Optional<CompanionScheduleDetail> result = adapter.findByMatchId(match.getId());

        assertThat(result).isPresent();
        assertThat(result.get().matchStatus()).isEqualTo(CompanionMatchStatus.CANCELED);
    }

    @Test
    void returnsEmptyWhenMatchDoesNotExist() {
        CompanionScheduleDetailQueryAdapter adapter = new CompanionScheduleDetailQueryAdapter(
                scheduleDetailJpaRepository
        );

        Optional<CompanionScheduleDetail> result = adapter.findByMatchId(999L);

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
        return new CompanionPostEntity(
                null,
                7L,
                placeId,
                NOW.plusDays(1),
                4,
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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionPostEntity.class,
            CompanionMatchEntity.class,
            CompanionScheduleEntity.class,
            PlaceCacheEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionPostJpaRepository.class,
            CompanionMatchJpaRepository.class,
            CompanionScheduleJpaRepository.class,
            CompanionScheduleDetailJpaRepository.class,
            PlaceCacheJpaRepository.class
    })
    static class TestApplication {
    }
}
