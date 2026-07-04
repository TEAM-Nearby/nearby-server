// 동행 매칭 참여자 저장소 어댑터의 참여 여부 조회를 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchParticipantEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchParticipantJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
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
class CompanionMatchParticipantPersistenceAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 4, 12, 0);

    @Autowired
    private CompanionPostJpaRepository companionPostJpaRepository;

    @Autowired
    private CompanionMatchJpaRepository companionMatchJpaRepository;

    @Autowired
    private CompanionMatchParticipantJpaRepository companionMatchParticipantJpaRepository;

    @Test
    void checksParticipantByMatchIdAndUserId() {
        CompanionMatchParticipantRepositoryAdapter adapter = new CompanionMatchParticipantRepositoryAdapter(
                companionMatchParticipantJpaRepository
        );
        CompanionPostEntity post = companionPostJpaRepository.saveAndFlush(new CompanionPostEntity(
                null,
                7L,
                30L,
                NOW.plusDays(1),
                4,
                "함께 밥 먹을 동행을 구해요.",
                "https://openchat.example",
                CompanionPostStatus.CLOSED,
                NOW
        ));
        CompanionMatchEntity match = companionMatchJpaRepository.saveAndFlush(new CompanionMatchEntity(
                null,
                post.getId(),
                CompanionMatchStatus.MATCHED,
                NOW
        ));
        Long userId = 987L;
        companionMatchParticipantJpaRepository.saveAndFlush(new CompanionMatchParticipantEntity(
                null,
                match.getId(),
                userId,
                null,
                MatchParticipantRole.HOST
        ));

        assertThat(adapter.existsByMatchIdAndUserId(match.getId(), userId)).isTrue();
        assertThat(adapter.existsByMatchIdAndUserId(match.getId(), 123L)).isFalse();
        assertThat(adapter.existsByMatchIdAndUserId(userId, match.getId())).isFalse();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionPostEntity.class,
            CompanionMatchEntity.class,
            CompanionMatchParticipantEntity.class,
            CompanionApplicationEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionPostJpaRepository.class,
            CompanionMatchJpaRepository.class,
            CompanionMatchParticipantJpaRepository.class
    })
    static class TestApplication {
    }
}
