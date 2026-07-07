// 진행 중인 동행 목록 조회 어댑터의 조인, 필터링, 매핑을 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchParticipantEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCheckInEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchParticipantJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MeetingCheckInJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.OngoingCompanionMeetingQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingSummary;
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
class OngoingCompanionMeetingQueryAdapterTest {

    private static final Long CURRENT_USER_ID = 7L;
    private static final Long HOST_USER_ID = 8L;
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 6, 29, 16, 30);

    @Autowired
    private CompanionPostJpaRepository postJpaRepository;

    @Autowired
    private CompanionMatchJpaRepository matchJpaRepository;

    @Autowired
    private CompanionMatchParticipantJpaRepository participantJpaRepository;

    @Autowired
    private CompanionMeetingJpaRepository meetingJpaRepository;

    @Autowired
    private CompanionScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private CompanionProfileJpaRepository profileJpaRepository;

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Autowired
    private MeetingCheckInJpaRepository checkInJpaRepository;

    @Autowired
    private OngoingCompanionMeetingQueryJpaRepository queryJpaRepository;

    @Test
    void findsOnlyOngoingMeetingsForCurrentUserWithHostProfileAndCheckInStatus() {
        OngoingCompanionMeetingQueryAdapter adapter = new OngoingCompanionMeetingQueryAdapter(queryJpaRepository);
        profileJpaRepository.saveAndFlush(profile(CURRENT_USER_ID, "정지영", UserGender.FEMALE));
        profileJpaRepository.saveAndFlush(profile(HOST_USER_ID, "김지원", UserGender.MALE));

        CompanionMeetingEntity currentUserHostMeeting = createMeeting(
                CURRENT_USER_ID,
                CURRENT_USER_ID,
                CompanionMeetingStatus.ONGOING,
                BASE_TIME.plusHours(1)
        );
        CompanionMeetingEntity currentUserGuestMeeting = createMeeting(
                HOST_USER_ID,
                CURRENT_USER_ID,
                CompanionMeetingStatus.ONGOING,
                BASE_TIME
        );
        checkInJpaRepository.saveAndFlush(checkIn(currentUserGuestMeeting.getId(), CURRENT_USER_ID));
        createMeeting(HOST_USER_ID, CURRENT_USER_ID, CompanionMeetingStatus.COMPLETED, BASE_TIME.plusHours(2));
        createMeeting(HOST_USER_ID, 99L, CompanionMeetingStatus.ONGOING, BASE_TIME.plusHours(3));

        List<OngoingCompanionMeetingSummary> result = adapter.findAllByParticipantUserId(CURRENT_USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).meetingId()).isEqualTo(currentUserHostMeeting.getId());
        assertThat(result.get(0).companion().userId()).isEqualTo(CURRENT_USER_ID);
        assertThat(result.get(0).companion().nickname()).isEqualTo("정지영");
        assertThat(result.get(0).companion().gender()).isEqualTo(UserGender.FEMALE);
        assertThat(result.get(0).checkedIn()).isFalse();
        assertThat(result.get(0).meetingStatus()).isEqualTo(CompanionMeetingStatus.ONGOING);
        assertThat(result.get(0).meetingAt()).isEqualTo(BASE_TIME.plusHours(1));

        assertThat(result.get(1).meetingId()).isEqualTo(currentUserGuestMeeting.getId());
        assertThat(result.get(1).companion().userId()).isEqualTo(HOST_USER_ID);
        assertThat(result.get(1).companion().nickname()).isEqualTo("김지원");
        assertThat(result.get(1).checkedIn()).isTrue();
        assertThat(result.get(1).placeName()).isEqualTo("시우다드 콘달");
    }

    @Test
    void returnsEmptyWhenUserHasNoOngoingMeetings() {
        OngoingCompanionMeetingQueryAdapter adapter = new OngoingCompanionMeetingQueryAdapter(queryJpaRepository);

        List<OngoingCompanionMeetingSummary> result = adapter.findAllByParticipantUserId(CURRENT_USER_ID);

        assertThat(result).isEmpty();
    }

    private CompanionMeetingEntity createMeeting(
            final Long hostUserId,
            final Long participantUserId,
            final CompanionMeetingStatus meetingStatus,
            final LocalDateTime scheduledAt
    ) {
        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place("google-place-" + scheduledAt));
        CompanionPostEntity post = postJpaRepository.saveAndFlush(post(hostUserId, place.getId(), scheduledAt));
        CompanionMatchEntity match = matchJpaRepository.saveAndFlush(match(post.getId()));
        participantJpaRepository.saveAndFlush(participant(match.getId(), hostUserId, MatchParticipantRole.HOST));
        if (!hostUserId.equals(participantUserId)) {
            participantJpaRepository.saveAndFlush(participant(match.getId(), participantUserId, MatchParticipantRole.GUEST));
        }
        scheduleJpaRepository.saveAndFlush(schedule(match.getId(), place.getId(), scheduledAt));
        return meetingJpaRepository.saveAndFlush(meeting(match.getId(), meetingStatus));
    }

    private CompanionProfileEntity profile(final Long userId, final String nickname, final UserGender gender) {
        return new CompanionProfileEntity(
                null,
                userId,
                nickname,
                gender,
                1998,
                "https://image.url/profile-" + userId + ".png",
                null,
                new BigDecimal("5.00"),
                0,
                CompanionProfileStatus.ACTIVE
        );
    }

    private PlaceCacheEntity place(final String googlePlaceId) {
        return new PlaceCacheEntity(
                null,
                googlePlaceId,
                "시우다드 콘달",
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

    private CompanionPostEntity post(final Long hostUserId, final Long placeId, final LocalDateTime meetingAt) {
        return new CompanionPostEntity(
                null,
                hostUserId,
                placeId,
                meetingAt,
                4,
                "함께 밥 먹을 동행을 구해요.",
                "https://openchat.example",
                CompanionPostStatus.CLOSED,
                BASE_TIME.minusDays(1)
        );
    }

    private CompanionMatchEntity match(final Long postId) {
        return new CompanionMatchEntity(null, postId, CompanionMatchStatus.SCHEDULE_CONFIRMED, BASE_TIME.minusDays(1));
    }

    private CompanionMatchParticipantEntity participant(
            final Long matchId,
            final Long userId,
            final MatchParticipantRole role
    ) {
        return new CompanionMatchParticipantEntity(null, matchId, userId, null, role);
    }

    private CompanionScheduleEntity schedule(final Long matchId, final Long placeId, final LocalDateTime scheduledAt) {
        return new CompanionScheduleEntity(null, matchId, placeId, scheduledAt, 120, true);
    }

    private CompanionMeetingEntity meeting(final Long matchId, final CompanionMeetingStatus status) {
        return new CompanionMeetingEntity(null, matchId, status, BASE_TIME.minusMinutes(5), null);
    }

    private MeetingCheckInEntity checkIn(final Long meetingId, final Long userId) {
        return new MeetingCheckInEntity(
                null,
                meetingId,
                userId,
                new BigDecimal("41.39020500"),
                new BigDecimal("2.16354800"),
                BASE_TIME.plusMinutes(5)
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionPostEntity.class,
            CompanionMatchEntity.class,
            CompanionMatchParticipantEntity.class,
            CompanionMeetingEntity.class,
            CompanionScheduleEntity.class,
            CompanionProfileEntity.class,
            MeetingCheckInEntity.class,
            PlaceCacheEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionPostJpaRepository.class,
            CompanionMatchJpaRepository.class,
            CompanionMatchParticipantJpaRepository.class,
            CompanionMeetingJpaRepository.class,
            CompanionScheduleJpaRepository.class,
            CompanionProfileJpaRepository.class,
            MeetingCheckInJpaRepository.class,
            PlaceCacheJpaRepository.class,
            OngoingCompanionMeetingQueryJpaRepository.class
    })
    static class TestApplication {
    }
}
