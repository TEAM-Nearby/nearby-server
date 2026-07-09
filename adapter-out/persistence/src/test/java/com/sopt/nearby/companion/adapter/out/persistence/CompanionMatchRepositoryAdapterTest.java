// 동행 매칭 저장소 어댑터의 조건부 일정 확정 상태 변경을 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionMatchRepositoryAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 5, 12, 0);

    @Autowired
    private CompanionPostJpaRepository companionPostJpaRepository;

    @Autowired
    private CompanionMatchJpaRepository companionMatchJpaRepository;

    @Test
    void confirmsScheduleOnlyWhenMatchIsMatched() {
        CompanionMatchRepositoryAdapter adapter = new CompanionMatchRepositoryAdapter(companionMatchJpaRepository);
        CompanionPostEntity post = companionPostJpaRepository.saveAndFlush(post());
        CompanionMatchEntity match = companionMatchJpaRepository.saveAndFlush(new CompanionMatchEntity(
                null,
                post.getId(),
                CompanionMatchStatus.MATCHED,
                NOW
        ));

        assertThat(adapter.confirmScheduleIfMatched(match.getId())).isTrue();
        assertThat(companionMatchJpaRepository.findById(match.getId()))
                .get()
                .extracting(CompanionMatchEntity::getStatus)
                .isEqualTo(CompanionMatchStatus.SCHEDULE_CONFIRMED);
        assertThat(adapter.confirmScheduleIfMatched(match.getId())).isFalse();
    }

    @Test
    void findsMatchedMatchByPostIdOnlyWhenStatusMatches() {
        CompanionMatchRepositoryAdapter adapter = new CompanionMatchRepositoryAdapter(companionMatchJpaRepository);
        CompanionPostEntity post = companionPostJpaRepository.saveAndFlush(post());
        companionMatchJpaRepository.saveAndFlush(new CompanionMatchEntity(
                null,
                post.getId(),
                CompanionMatchStatus.SCHEDULE_CONFIRMED,
                NOW.minusMinutes(1)
        ));
        CompanionMatchEntity matched = companionMatchJpaRepository.saveAndFlush(new CompanionMatchEntity(
                null,
                post.getId(),
                CompanionMatchStatus.MATCHED,
                NOW
        ));

        assertThat(adapter.findFirstByPostIdAndStatus(post.getId(), CompanionMatchStatus.MATCHED))
                .get()
                .extracting(CompanionMatch::id)
                .isEqualTo(matched.getId());
        assertThat(adapter.findFirstByPostIdAndStatus(post.getId(), CompanionMatchStatus.CANCELED)).isEmpty();
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
            CompanionMatchEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionPostJpaRepository.class,
            CompanionMatchJpaRepository.class
    })
    static class TestApplication {
    }
}
