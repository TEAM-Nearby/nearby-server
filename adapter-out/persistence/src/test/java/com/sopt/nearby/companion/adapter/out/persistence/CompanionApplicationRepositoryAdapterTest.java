// 동행 신청 저장소 어댑터의 저장, 조회, 중복 신청 방지를 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionApplicationJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.domain.exception.CompanionRequestAlreadyExistsException;
import com.sopt.nearby.companion.domain.model.match.CompanionApplication;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionApplicationRepositoryAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 15, 12, 30);

    @Autowired
    private CompanionPostJpaRepository postJpaRepository;

    @Autowired
    private CompanionApplicationJpaRepository applicationJpaRepository;

    @Test
    void savesApplicationAndChecksExistingPostApplicantPair() {
        CompanionApplicationRepositoryAdapter adapter = new CompanionApplicationRepositoryAdapter(applicationJpaRepository);
        CompanionPostEntity post = postJpaRepository.saveAndFlush(post());

        CompanionApplication saved = adapter.save(application(post.getId(), 7L));

        assertThat(saved.id()).isNotNull();
        assertThat(adapter.findById(saved.id())).isPresent();
        assertThat(adapter.existsByPostIdAndApplicantUserId(post.getId(), 7L)).isTrue();
        assertThat(adapter.existsByPostIdAndApplicantUserId(post.getId(), 8L)).isFalse();
    }

    @Test
    void throwsAlreadyExistsWhenSamePostApplicantPairIsSavedAgain() {
        CompanionApplicationRepositoryAdapter adapter = new CompanionApplicationRepositoryAdapter(applicationJpaRepository);
        CompanionPostEntity post = postJpaRepository.saveAndFlush(post());
        adapter.save(application(post.getId(), 7L));

        assertThatThrownBy(() -> adapter.save(application(post.getId(), 7L)))
                .isInstanceOf(CompanionRequestAlreadyExistsException.class);
    }

    @Test
    void rethrowsDataIntegrityViolationWhenPostDoesNotExist() {
        CompanionApplicationRepositoryAdapter adapter = new CompanionApplicationRepositoryAdapter(applicationJpaRepository);

        assertThatThrownBy(() -> adapter.save(application(999L, 7L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private CompanionPostEntity post() {
        return new CompanionPostEntity(
                null,
                100L,
                20L,
                NOW.plusHours(2),
                4,
                "같이 밥 먹을 동행을 구해요.",
                "https://open.kakao.com/o/nearby123",
                CompanionPostStatus.RECRUITING,
                NOW.minusHours(1)
        );
    }

    private CompanionApplication application(final Long postId, final Long applicantUserId) {
        return new CompanionApplication(
                null,
                postId,
                applicantUserId,
                CompanionApplicationStatus.PENDING,
                null,
                NOW
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionPostEntity.class,
            CompanionApplicationEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionPostJpaRepository.class,
            CompanionApplicationJpaRepository.class
    })
    static class TestApplication {
    }
}
