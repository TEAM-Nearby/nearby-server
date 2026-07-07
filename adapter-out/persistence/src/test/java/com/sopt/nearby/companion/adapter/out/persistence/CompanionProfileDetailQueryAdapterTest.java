// 동행 프로필 상세 조회 쿼리 어댑터의 조인 결과와 ACTIVE 필터를 검증한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileStyleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileDetailQueryJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileStyleJpaRepository;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileDetail;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.user.adapter.out.persistence.entity.UserAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.repository.UserAccountJpaRepository;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
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
class CompanionProfileDetailQueryAdapterTest {

    @Autowired
    private UserAccountJpaRepository userAccountJpaRepository;

    @Autowired
    private CompanionProfileJpaRepository companionProfileJpaRepository;

    @Autowired
    private CompanionProfileStyleJpaRepository companionProfileStyleJpaRepository;

    @Autowired
    private CompanionProfileDetailQueryJpaRepository companionProfileDetailQueryJpaRepository;

    @Test
    void findsActiveProfileDetailWithPhoneVerificationAndKeywords() {
        CompanionProfileDetailQueryAdapter adapter =
                new CompanionProfileDetailQueryAdapter(companionProfileDetailQueryJpaRepository);
        UserAccountEntity user = userAccountJpaRepository.saveAndFlush(user(LocalDateTime.of(2026, 7, 1, 10, 0)));
        CompanionProfileEntity profile = companionProfileJpaRepository.saveAndFlush(profile(
                user.getId(),
                "니어바이",
                CompanionProfileStatus.ACTIVE
        ));
        companionProfileStyleJpaRepository.saveAndFlush(new CompanionProfileStyleEntity(
                profile.getId(),
                TravelStyleKeyword.PLANNED
        ));
        companionProfileStyleJpaRepository.saveAndFlush(new CompanionProfileStyleEntity(
                profile.getId(),
                TravelStyleKeyword.FOODIE
        ));

        CompanionProfileDetail result = adapter.findByProfileId(profile.getId()).orElseThrow();

        assertThat(result.profileId()).isEqualTo(profile.getId());
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.nickname()).isEqualTo("니어바이");
        assertThat(result.gender()).isEqualTo(UserGender.FEMALE);
        assertThat(result.birthYear()).isNull();
        assertThat(result.profileImageUrl()).isEqualTo("https://cdn.nearby.com/profiles/1.jpg");
        assertThat(result.intro()).isEqualTo("혼자 여행도 같이 여행도 좋아해요");
        assertThat(result.mannerScore()).isEqualByComparingTo("4.00");
        assertThat(result.reviewCount()).isEqualTo(12);
        assertThat(result.status()).isEqualTo(CompanionProfileStatus.ACTIVE);
        assertThat(result.phoneVerifiedAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 0));
        assertThat(result.keywords()).containsExactly(TravelStyleKeyword.FOODIE, TravelStyleKeyword.PLANNED);
    }

    @Test
    void returnsEmptyWhenProfileIsInactive() {
        assertNotActiveProfileIsHidden(CompanionProfileStatus.INACTIVE, "비활성");
    }

    @Test
    void returnsEmptyWhenProfileIsSkipped() {
        assertNotActiveProfileIsHidden(CompanionProfileStatus.SKIPPED, "건너뜀");
    }

    @Test
    void returnsEmptyWhenProfileDoesNotExist() {
        CompanionProfileDetailQueryAdapter adapter =
                new CompanionProfileDetailQueryAdapter(companionProfileDetailQueryJpaRepository);

        Optional<CompanionProfileDetail> result = adapter.findByProfileId(999L);

        assertThat(result).isEmpty();
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

    private void assertNotActiveProfileIsHidden(
            final CompanionProfileStatus status,
            final String nickname
    ) {
        CompanionProfileDetailQueryAdapter adapter =
                new CompanionProfileDetailQueryAdapter(companionProfileDetailQueryJpaRepository);
        UserAccountEntity user = userAccountJpaRepository.saveAndFlush(user(null));
        CompanionProfileEntity profile = companionProfileJpaRepository.saveAndFlush(profile(
                user.getId(),
                nickname,
                status
        ));

        Optional<CompanionProfileDetail> result = adapter.findByProfileId(profile.getId());

        assertThat(result).isEmpty();
    }

    private CompanionProfileEntity profile(
            final Long userId,
            final String nickname,
            final CompanionProfileStatus status
    ) {
        return new CompanionProfileEntity(
                null,
                userId,
                nickname,
                UserGender.FEMALE,
                null,
                "https://cdn.nearby.com/profiles/1.jpg",
                "혼자 여행도 같이 여행도 좋아해요",
                new BigDecimal("4.00"),
                12,
                status
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            UserAccountEntity.class,
            CompanionProfileEntity.class,
            CompanionProfileStyleEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            UserAccountJpaRepository.class,
            CompanionProfileJpaRepository.class,
            CompanionProfileStyleJpaRepository.class,
            CompanionProfileDetailQueryJpaRepository.class
    })
    static class TestApplication {
    }
}
