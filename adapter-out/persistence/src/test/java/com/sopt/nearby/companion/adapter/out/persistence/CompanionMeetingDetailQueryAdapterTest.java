// 동행 상세 조회 어댑터의 호스트 프로필과 체크인 상태 매핑을 검증하는 테스트
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
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingDetailQueryJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MeetingCheckInJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingDetail;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
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
class CompanionMeetingDetailQueryAdapterTest {

    private static final Long HOST_USER_ID = 1L;
    private static final Long GUEST_USER_ID = 7L;
    private static final LocalDateTime MEETING_AT = LocalDateTime.of(2026, 6, 29, 18, 30);

    @Autowired
    private CompanionPostJpaRepository postJpaRepository;

    @Autowired
    private CompanionMatchJpaRepository matchJpaRepository;

    @Autowired
    private CompanionMeetingJpaRepository meetingJpaRepository;

    @Autowired
    private CompanionScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private CompanionMatchParticipantJpaRepository participantJpaRepository;

    @Autowired
    private CompanionProfileJpaRepository profileJpaRepository;

    @Autowired
    private MeetingCheckInJpaRepository checkInJpaRepository;

    @Autowired
    private CompanionMeetingDetailQueryJpaRepository queryJpaRepository;

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Test
    void findsOngoingMeetingDetailWithHostGenderAndCheckInState() {
        CompanionMeetingDetailQueryAdapter adapter = new CompanionMeetingDetailQueryAdapter(queryJpaRepository);
        TestFixture fixture = saveFixture(CompanionMeetingStatus.ONGOING);
        checkInJpaRepository.saveAndFlush(checkIn(fixture.meeting().getId(), HOST_USER_ID));

        Optional<CompanionMeetingDetail> result = adapter.findByMeetingIdAndUserId(
                fixture.meeting().getId(),
                GUEST_USER_ID
        );

        assertThat(result).isPresent();
        CompanionMeetingDetail detail = result.get();
        assertThat(detail.meetingId()).isEqualTo(fixture.meeting().getId());
        assertThat(detail.currentUserRole()).isEqualTo(MatchParticipantRole.GUEST);
        assertThat(detail.hostId()).isEqualTo(HOST_USER_ID);
        assertThat(detail.hostGender()).isEqualTo(UserGender.FEMALE);
        assertThat(detail.hostProfileImageUrl()).isEqualTo("https://image.url/profile.png");
        assertThat(detail.hostNickname()).isEqualTo("정지영");
        assertThat(detail.hostCheckedIn()).isTrue();
        assertThat(detail.placeName()).isEqualTo("시우다드 콘달");
        assertThat(detail.meetingAt()).isEqualTo(MEETING_AT);
        assertThat(detail.meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.SCHEDULED);
        assertThat(detail.meetingStatus()).isEqualTo(CompanionMeetingStatus.ONGOING);
        assertThat(detail.currentUserCheckedIn()).isFalse();
    }

    @Test
    void resolvesNowMeetingAtFromExposureExpiresAtAndReturnsMeetingTimeType() {
        CompanionMeetingDetailQueryAdapter adapter = new CompanionMeetingDetailQueryAdapter(queryJpaRepository);
        LocalDateTime exposureExpiresAt = MEETING_AT.minusHours(1);
        TestFixture fixture = saveFixture(
                CompanionMeetingStatus.ONGOING,
                CompanionPostMeetingTimeType.NOW,
                null,
                exposureExpiresAt,
                MEETING_AT
        );

        Optional<CompanionMeetingDetail> result = adapter.findByMeetingIdAndUserId(
                fixture.meeting().getId(),
                GUEST_USER_ID
        );

        assertThat(result).isPresent();
        assertThat(result.get().meetingAt()).isEqualTo(exposureExpiresAt);
        assertThat(result.get().meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.NOW);
    }

    @Test
    void returnsScheduledMeetingTimeTypeForUndecidedPostAfterScheduleIsConfirmed() {
        CompanionMeetingDetailQueryAdapter adapter = new CompanionMeetingDetailQueryAdapter(queryJpaRepository);
        TestFixture fixture = saveFixture(
                CompanionMeetingStatus.ONGOING,
                CompanionPostMeetingTimeType.UNDECIDED,
                null,
                null,
                MEETING_AT
        );

        Optional<CompanionMeetingDetail> result = adapter.findByMeetingIdAndUserId(
                fixture.meeting().getId(),
                GUEST_USER_ID
        );

        assertThat(result).isPresent();
        assertThat(result.get().meetingAt()).isEqualTo(MEETING_AT);
        assertThat(result.get().meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.SCHEDULED);
    }

    @Test
    void returnsHostRoleAndHostProfileWhenRequesterIsHost() {
        CompanionMeetingDetailQueryAdapter adapter = new CompanionMeetingDetailQueryAdapter(queryJpaRepository);
        TestFixture fixture = saveFixture(CompanionMeetingStatus.ONGOING);
        checkInJpaRepository.saveAndFlush(checkIn(fixture.meeting().getId(), HOST_USER_ID));

        Optional<CompanionMeetingDetail> result = adapter.findByMeetingIdAndUserId(
                fixture.meeting().getId(),
                HOST_USER_ID
        );

        assertThat(result).isPresent();
        assertThat(result.get().currentUserRole()).isEqualTo(MatchParticipantRole.HOST);
        assertThat(result.get().hostId()).isEqualTo(HOST_USER_ID);
        assertThat(result.get().hostGender()).isEqualTo(UserGender.FEMALE);
        assertThat(result.get().currentUserCheckedIn()).isTrue();
    }

    @Test
    void returnsDetailWithNullRoleWhenRequesterIsNotParticipant() {
        CompanionMeetingDetailQueryAdapter adapter = new CompanionMeetingDetailQueryAdapter(queryJpaRepository);
        TestFixture fixture = saveFixture(CompanionMeetingStatus.ONGOING);

        Optional<CompanionMeetingDetail> result = adapter.findByMeetingIdAndUserId(
                fixture.meeting().getId(),
                99L
        );

        assertThat(result).isPresent();
        assertThat(result.get().currentUserRole()).isNull();
        assertThat(result.get().currentUserCheckedIn()).isFalse();
    }

    @Test
    void returnsMeetingStatusSoServiceCanRejectNonOngoingMeetings() {
        CompanionMeetingDetailQueryAdapter adapter = new CompanionMeetingDetailQueryAdapter(queryJpaRepository);
        TestFixture fixture = saveFixture(CompanionMeetingStatus.COMPLETED);

        Optional<CompanionMeetingDetail> result = adapter.findByMeetingIdAndUserId(
                fixture.meeting().getId(),
                GUEST_USER_ID
        );

        assertThat(result).isPresent();
        assertThat(result.get().meetingStatus()).isEqualTo(CompanionMeetingStatus.COMPLETED);
    }

    @Test
    void returnsEmptyWhenMeetingDoesNotExist() {
        CompanionMeetingDetailQueryAdapter adapter = new CompanionMeetingDetailQueryAdapter(queryJpaRepository);

        Optional<CompanionMeetingDetail> result = adapter.findByMeetingIdAndUserId(999L, GUEST_USER_ID);

        assertThat(result).isEmpty();
    }

    private TestFixture saveFixture(final CompanionMeetingStatus meetingStatus) {
        return saveFixture(
                meetingStatus,
                CompanionPostMeetingTimeType.SCHEDULED,
                MEETING_AT,
                null,
                MEETING_AT
        );
    }

    private TestFixture saveFixture(
            final CompanionMeetingStatus meetingStatus,
            final CompanionPostMeetingTimeType meetingTimeType,
            final LocalDateTime postMeetingAt,
            final LocalDateTime exposureExpiresAt,
            final LocalDateTime scheduledAt
    ) {
        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place());
        profileJpaRepository.saveAndFlush(profile(HOST_USER_ID, "정지영", UserGender.FEMALE));
        profileJpaRepository.saveAndFlush(profile(GUEST_USER_ID, "동행자", UserGender.MALE));
        CompanionPostEntity post = postJpaRepository.saveAndFlush(post(
                place.getId(),
                meetingTimeType,
                postMeetingAt,
                exposureExpiresAt
        ));
        CompanionMatchEntity match = matchJpaRepository.saveAndFlush(match(post.getId()));
        participantJpaRepository.saveAndFlush(participant(match.getId(), HOST_USER_ID, MatchParticipantRole.HOST));
        participantJpaRepository.saveAndFlush(participant(match.getId(), GUEST_USER_ID, MatchParticipantRole.GUEST));
        CompanionMeetingEntity meeting = meetingJpaRepository.saveAndFlush(meeting(match.getId(), meetingStatus));
        scheduleJpaRepository.saveAndFlush(schedule(match.getId(), place.getId(), scheduledAt));
        return new TestFixture(meeting);
    }

    private PlaceCacheEntity place() {
        return new PlaceCacheEntity(
                null,
                "google-place-id",
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

    private CompanionProfileEntity profile(final Long userId, final String nickname, final UserGender gender) {
        return new CompanionProfileEntity(
                null,
                userId,
                nickname,
                gender,
                1998,
                "https://image.url/profile.png",
                "반갑습니다.",
                new BigDecimal("4.50"),
                3,
                CompanionProfileStatus.ACTIVE
        );
    }

    private CompanionPostEntity post(final Long placeId) {
        return post(placeId, CompanionPostMeetingTimeType.SCHEDULED, MEETING_AT, null);
    }

    private CompanionPostEntity post(
            final Long placeId,
            final CompanionPostMeetingTimeType meetingTimeType,
            final LocalDateTime meetingAt,
            final LocalDateTime exposureExpiresAt
    ) {
        return new CompanionPostEntity(
                null,
                HOST_USER_ID,
                placeId,
                meetingTimeType,
                meetingAt,
                exposureExpiresAt,
                4,
                true,
                "함께 밥 먹을 동행을 구해요.",
                "https://openchat.example",
                CompanionPostStatus.CLOSED,
                MEETING_AT.minusDays(1)
        );
    }

    private CompanionMatchEntity match(final Long postId) {
        return new CompanionMatchEntity(null, postId, CompanionMatchStatus.SCHEDULE_CONFIRMED, MEETING_AT.minusDays(1));
    }

    private CompanionMatchParticipantEntity participant(
            final Long matchId,
            final Long userId,
            final MatchParticipantRole role
    ) {
        return new CompanionMatchParticipantEntity(null, matchId, userId, null, role);
    }

    private CompanionMeetingEntity meeting(final Long matchId, final CompanionMeetingStatus status) {
        return new CompanionMeetingEntity(null, matchId, status, MEETING_AT.minusMinutes(5), null);
    }

    private CompanionScheduleEntity schedule(
            final Long matchId,
            final Long placeId,
            final LocalDateTime scheduledAt
    ) {
        return new CompanionScheduleEntity(null, matchId, placeId, scheduledAt, 120, true);
    }

    private MeetingCheckInEntity checkIn(final Long meetingId, final Long userId) {
        return new MeetingCheckInEntity(
                null,
                meetingId,
                userId,
                new BigDecimal("41.39020500"),
                new BigDecimal("2.16354800"),
                MEETING_AT.plusMinutes(5)
        );
    }

    private record TestFixture(CompanionMeetingEntity meeting) {
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
            CompanionMeetingDetailQueryJpaRepository.class,
            PlaceCacheJpaRepository.class
    })
    static class TestApplication {
    }
}
