// 만남 인증 저장소 어댑터의 조회, 카운트, 중복 저장 방지를 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCheckInEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MeetingCheckInJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class MeetingCheckInRepositoryAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 29, 18, 35);

    @Autowired
    private CompanionPostJpaRepository postJpaRepository;

    @Autowired
    private CompanionMatchJpaRepository matchJpaRepository;

    @Autowired
    private CompanionMeetingJpaRepository meetingJpaRepository;

    @Autowired
    private MeetingCheckInJpaRepository checkInJpaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndFindsCheckInByMeetingIdAndUserId() {
        MeetingCheckInRepositoryAdapter adapter = new MeetingCheckInRepositoryAdapter(
                checkInJpaRepository,
                entityManager
        );
        CompanionMeetingEntity meeting = meetingJpaRepository.saveAndFlush(meeting(match().getId()));

        MeetingCheckIn saved = adapter.saveIfAbsent(checkIn(meeting.getId(), 7L, NOW));

        assertThat(saved.id()).isNotNull();
        assertThat(adapter.findByMeetingIdAndUserId(meeting.getId(), 7L)).isPresent();
        assertThat(adapter.countByMeetingId(meeting.getId())).isEqualTo(1L);
    }

    @Test
    void returnsExistingCheckInWhenDuplicateSaveIsRequested() {
        MeetingCheckInRepositoryAdapter adapter = new MeetingCheckInRepositoryAdapter(
                checkInJpaRepository,
                entityManager
        );
        CompanionMeetingEntity meeting = meetingJpaRepository.saveAndFlush(meeting(match().getId()));
        MeetingCheckIn first = adapter.saveIfAbsent(checkIn(meeting.getId(), 7L, NOW.minusMinutes(5)));

        MeetingCheckIn duplicate = adapter.saveIfAbsent(checkIn(meeting.getId(), 7L, NOW));

        assertThat(duplicate.id()).isEqualTo(first.id());
        assertThat(duplicate.checkedInAt()).isEqualTo(NOW.minusMinutes(5));
        assertThat(adapter.countByMeetingId(meeting.getId())).isEqualTo(1L);
    }

    private CompanionMatchEntity match() {
        CompanionPostEntity post = postJpaRepository.saveAndFlush(new CompanionPostEntity(
                null,
                7L,
                30L,
                NOW,
                4,
                "함께 밥 먹을 동행을 구해요.",
                "https://openchat.example",
                CompanionPostStatus.CLOSED,
                NOW.minusDays(1)
        ));
        return matchJpaRepository.saveAndFlush(new CompanionMatchEntity(
                null,
                post.getId(),
                CompanionMatchStatus.SCHEDULE_CONFIRMED,
                NOW.minusDays(1)
        ));
    }

    private CompanionMeetingEntity meeting(final Long matchId) {
        return new CompanionMeetingEntity(null, matchId, CompanionMeetingStatus.ONGOING, NOW.minusMinutes(5), null);
    }

    private MeetingCheckIn checkIn(final Long meetingId, final Long userId, final LocalDateTime checkedInAt) {
        return new MeetingCheckIn(
                null,
                meetingId,
                userId,
                new BigDecimal("41.39020500"),
                new BigDecimal("2.16354800"),
                checkedInAt
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionPostEntity.class,
            CompanionMatchEntity.class,
            CompanionMeetingEntity.class,
            MeetingCheckInEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionPostJpaRepository.class,
            CompanionMatchJpaRepository.class,
            CompanionMeetingJpaRepository.class,
            MeetingCheckInJpaRepository.class
    })
    static class TestApplication {
    }
}
