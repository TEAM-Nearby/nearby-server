// 동행 모집 글 저장소 어댑터의 신규 작성 필드 매핑을 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
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
class CompanionPostRepositoryAdapterTest {

    @Autowired
    private CompanionPostJpaRepository companionPostJpaRepository;

    @Test
    void savesAndLoadsCreationFields() {
        CompanionPostRepositoryAdapter adapter = new CompanionPostRepositoryAdapter(companionPostJpaRepository);
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 2, 14, 0);
        LocalDateTime exposureExpiresAt = createdAt.plusHours(1);

        CompanionPost saved = adapter.save(new CompanionPost(
                null,
                7L,
                20L,
                CompanionPostMeetingTimeType.NOW,
                null,
                exposureExpiresAt,
                4,
                false,
                "같이 스시 먹으러 갈 사람 구해요.",
                "https://open.kakao.com/o/nearby123",
                CompanionPostStatus.RECRUITING,
                createdAt
        ));

        CompanionPost found = adapter.findById(saved.id()).orElseThrow();

        assertThat(found.meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.NOW);
        assertThat(found.meetingAt()).isNull();
        assertThat(found.exposureExpiresAt()).isEqualTo(exposureExpiresAt);
        assertThat(found.departEvenIfNotFull()).isFalse();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = CompanionPostEntity.class)
    @EnableJpaRepositories(basePackageClasses = CompanionPostJpaRepository.class)
    static class TestApplication {
    }
}
