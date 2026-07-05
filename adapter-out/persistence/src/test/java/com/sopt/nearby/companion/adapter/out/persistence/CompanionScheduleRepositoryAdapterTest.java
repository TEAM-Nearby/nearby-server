// 동행 일정 저장소 어댑터의 확정 일정 조회를 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionSchedule;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
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
class CompanionScheduleRepositoryAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 5, 12, 0);

    @Autowired
    private CompanionPostJpaRepository companionPostJpaRepository;

    @Autowired
    private CompanionMatchJpaRepository companionMatchJpaRepository;

    @Autowired
    private CompanionScheduleJpaRepository companionScheduleJpaRepository;

    @Test
    void findsConfirmedScheduleByMatchId() {
        CompanionScheduleRepositoryAdapter adapter = new CompanionScheduleRepositoryAdapter(
                companionScheduleJpaRepository
        );
        CompanionMatchEntity match = companionMatchJpaRepository.saveAndFlush(new CompanionMatchEntity(
                null,
                companionPostJpaRepository.saveAndFlush(post()).getId(),
                CompanionMatchStatus.MATCHED,
                NOW
        ));
        companionScheduleJpaRepository.saveAndFlush(new CompanionScheduleEntity(
                null,
                match.getId(),
                30L,
                NOW.plusDays(1),
                120,
                false
        ));
        CompanionScheduleEntity confirmedSchedule = companionScheduleJpaRepository.saveAndFlush(
                new CompanionScheduleEntity(
                        null,
                        match.getId(),
                        40L,
                        NOW.plusDays(2),
                        90,
                        true
                )
        );

        Optional<CompanionSchedule> result = adapter.findConfirmedByMatchId(match.getId());

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(confirmedSchedule.getId());
        assertThat(result.get().placeId()).isEqualTo(40L);
        assertThat(result.get().scheduledAt()).isEqualTo(NOW.plusDays(2));
        assertThat(result.get().confirmed()).isTrue();
    }

    private CompanionPostEntity post() {
        return new CompanionPostEntity(
                null,
                7L,
                30L,
                NOW.plusDays(1),
                4,
                "함께 밥 먹을 동행을 구해요.",
                "https://openchat.example",
                CompanionPostStatus.CLOSED,
                NOW
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionPostEntity.class,
            CompanionMatchEntity.class,
            CompanionScheduleEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionPostJpaRepository.class,
            CompanionMatchJpaRepository.class,
            CompanionScheduleJpaRepository.class
    })
    static class TestApplication {
    }
}
