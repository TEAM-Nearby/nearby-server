// 매칭된 동행 목록 조회 어댑터의 조인 쿼리와 매핑을 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchParticipantEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchParticipantJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchSummaryJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionMatchSummaryQueryAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 4, 12, 0);

    @Autowired
    private CompanionMatchJpaRepository companionMatchJpaRepository;

    @Autowired
    private CompanionMatchParticipantJpaRepository participantJpaRepository;

    @Autowired
    private CompanionPostJpaRepository companionPostJpaRepository;

    @Autowired
    private CompanionProfileJpaRepository companionProfileJpaRepository;

    @Autowired
    private CompanionScheduleJpaRepository companionScheduleJpaRepository;

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Autowired
    private CompanionMatchSummaryJpaRepository summaryJpaRepository;

    @Test
    void findsParticipantMatchesWithHostNicknamePlaceNameAndResolvedMeetingAtOrderByMatchCreatedAtDesc() {
        CompanionMatchSummaryQueryAdapter adapter = new CompanionMatchSummaryQueryAdapter(summaryJpaRepository);

        PlaceCacheEntity postPlace = placeCacheJpaRepository.saveAndFlush(place("post-place", "모집글 장소"));
        PlaceCacheEntity schedulePlace = placeCacheJpaRepository.saveAndFlush(place("schedule-place", "확정 장소"));
        PlaceCacheEntity oldPostPlace = placeCacheJpaRepository.saveAndFlush(place("old-post-place", "오래된 모집글 장소"));

        companionProfileJpaRepository.saveAndFlush(profile(100L, "호스트A"));
        companionProfileJpaRepository.saveAndFlush(profile(200L, "호스트B"));

        CompanionPostEntity scheduledPost = companionPostJpaRepository.saveAndFlush(post(
                100L,
                postPlace.getId(),
                NOW.plusDays(1),
                "확정 일정이 있는 모집글"
        ));
        CompanionMatchEntity recentMatch = companionMatchJpaRepository.saveAndFlush(match(
                scheduledPost.getId(),
                CompanionMatchStatus.SCHEDULE_CONFIRMED,
                NOW.plusHours(2)
        ));
        participantJpaRepository.saveAndFlush(participant(recentMatch.getId(), 7L));
        companionScheduleJpaRepository.saveAndFlush(schedule(
                recentMatch.getId(),
                schedulePlace.getId(),
                NOW.plusDays(3),
                true
        ));

        CompanionPostEntity oldPost = companionPostJpaRepository.saveAndFlush(post(
                200L,
                oldPostPlace.getId(),
                NOW.plusDays(5),
                "확정 일정이 없는 모집글"
        ));
        CompanionMatchEntity oldMatch = companionMatchJpaRepository.saveAndFlush(match(
                oldPost.getId(),
                CompanionMatchStatus.MATCHED,
                NOW.minusHours(1)
        ));
        participantJpaRepository.saveAndFlush(participant(oldMatch.getId(), 7L));

        CompanionPostEntity otherUserPost = companionPostJpaRepository.saveAndFlush(post(
                100L,
                postPlace.getId(),
                NOW.plusDays(10),
                "다른 유저 매칭"
        ));
        CompanionMatchEntity otherUserMatch = companionMatchJpaRepository.saveAndFlush(match(
                otherUserPost.getId(),
                CompanionMatchStatus.MATCHED,
                NOW.plusHours(3)
        ));
        participantJpaRepository.saveAndFlush(participant(otherUserMatch.getId(), 999L));

        CompanionPostEntity canceledPost = companionPostJpaRepository.saveAndFlush(post(
                100L,
                postPlace.getId(),
                NOW.plusDays(10),
                "취소된 매칭"
        ));
        CompanionMatchEntity canceledMatch = companionMatchJpaRepository.saveAndFlush(match(
                canceledPost.getId(),
                CompanionMatchStatus.CANCELED,
                NOW.plusHours(4)
        ));
        participantJpaRepository.saveAndFlush(participant(canceledMatch.getId(), 7L));

        List<CompanionMatchSummary> result = adapter.findAllByParticipantUserId(7L);

        assertThat(result).hasSize(2);

        CompanionMatchSummary first = result.get(0);
        assertThat(first.matchId()).isEqualTo(recentMatch.getId());
        assertThat(first.hostNickname()).isEqualTo("호스트A");
        assertThat(first.hostProfileImageUrl()).isEqualTo("https://image.example/100.png");
        assertThat(first.hostGender()).isEqualTo(UserGender.FEMALE);
        assertThat(first.placeName()).isEqualTo("확정 장소");
        assertThat(first.meetingAt()).isEqualTo(NOW.plusDays(3));
        assertThat(first.meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.SCHEDULED);
        assertThat(first.createdAt()).isEqualTo(NOW);
        assertThat(first.content()).isEqualTo("확정 일정이 있는 모집글");
        assertThat(first.matchStatus()).isEqualTo(CompanionMatchStatus.SCHEDULE_CONFIRMED);

        CompanionMatchSummary second = result.get(1);
        assertThat(second.matchId()).isEqualTo(oldMatch.getId());
        assertThat(second.hostNickname()).isEqualTo("호스트B");
        assertThat(second.hostProfileImageUrl()).isEqualTo("https://image.example/200.png");
        assertThat(second.hostGender()).isEqualTo(UserGender.FEMALE);
        assertThat(second.placeName()).isEqualTo("오래된 모집글 장소");
        assertThat(second.meetingAt()).isEqualTo(NOW.plusDays(5));
        assertThat(second.meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.SCHEDULED);
        assertThat(second.createdAt()).isEqualTo(NOW);
        assertThat(second.content()).isEqualTo("확정 일정이 없는 모집글");
        assertThat(second.matchStatus()).isEqualTo(CompanionMatchStatus.MATCHED);
    }

    private PlaceCacheEntity place(final String googlePlaceId, final String name) {
        return new PlaceCacheEntity(
                null,
                googlePlaceId,
                name,
                "서울시 어딘가",
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                "restaurant",
                null,
                new BigDecimal("4.50"),
                10,
                null,
                PlaceBusinessStatus.OPERATIONAL
        );
    }

    private CompanionProfileEntity profile(final Long userId, final String nickname) {
        return new CompanionProfileEntity(
                null,
                userId,
                nickname,
                UserGender.FEMALE,
                2000,
                "https://image.example/" + userId + ".png",
                "반가워요.",
                new BigDecimal("4.50"),
                0,
                CompanionProfileStatus.ACTIVE
        );
    }

    private CompanionPostEntity post(
            final Long hostUserId,
            final Long placeId,
            final LocalDateTime meetingAt,
            final String content
    ) {
        return new CompanionPostEntity(
                null,
                hostUserId,
                placeId,
                meetingAt,
                4,
                content,
                "https://openchat.example",
                CompanionPostStatus.CLOSED,
                NOW
        );
    }

    private CompanionMatchEntity match(
            final Long postId,
            final CompanionMatchStatus status,
            final LocalDateTime createdAt
    ) {
        return new CompanionMatchEntity(
                null,
                postId,
                status,
                createdAt
        );
    }

    private CompanionMatchParticipantEntity participant(final Long matchId, final Long userId) {
        return new CompanionMatchParticipantEntity(
                null,
                matchId,
                userId,
                null,
                MatchParticipantRole.GUEST
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
            CompanionMatchEntity.class,
            CompanionMatchParticipantEntity.class,
            CompanionPostEntity.class,
            CompanionProfileEntity.class,
            CompanionScheduleEntity.class,
            PlaceCacheEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionMatchJpaRepository.class,
            CompanionMatchParticipantJpaRepository.class,
            CompanionPostJpaRepository.class,
            CompanionProfileJpaRepository.class,
            CompanionScheduleJpaRepository.class,
            CompanionMatchSummaryJpaRepository.class,
            PlaceCacheJpaRepository.class
    })
    static class TestApplication {
    }
}
