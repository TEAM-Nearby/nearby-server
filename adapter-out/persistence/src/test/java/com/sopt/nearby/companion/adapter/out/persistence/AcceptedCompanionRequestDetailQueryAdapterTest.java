// 수락된 동행 신청 상세 조회의 소유권과 일정 우선순위를 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchParticipantEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.AcceptedCompanionRequestDetailJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionApplicationJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchParticipantJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.companion.domain.model.match.AcceptedCompanionRequestDetail;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class AcceptedCompanionRequestDetailQueryAdapterTest {

    private static final Long HOST_USER_ID = 1L;
    private static final Long REQUESTER_USER_ID = 7L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 7, 1, 12, 0);

    @Autowired
    private CompanionApplicationJpaRepository applicationRepository;

    @Autowired
    private CompanionPostJpaRepository postRepository;

    @Autowired
    private CompanionMatchJpaRepository matchRepository;

    @Autowired
    private CompanionMatchParticipantJpaRepository participantRepository;

    @Autowired
    private CompanionScheduleJpaRepository scheduleRepository;

    @Autowired
    private CompanionProfileJpaRepository profileRepository;

    @Autowired
    private PlaceCacheJpaRepository placeRepository;

    @Autowired
    private AcceptedCompanionRequestDetailJpaRepository queryRepository;

    @Test
    void findsOnlyOwnAcceptedApplicationWithConfirmedScheduleAndExactCounts() {
        PlaceCacheEntity postPlace = placeRepository.saveAndFlush(place(
                "post-place", "모집 장소", "모집 주소", "37.10000000", "127.10000000"));
        PlaceCacheEntity confirmedPlace = placeRepository.saveAndFlush(place(
                "confirmed-place", "확정 장소", "확정 주소", "37.20000000", "127.20000000"));
        profileRepository.saveAndFlush(profile(HOST_USER_ID, "호스트", "https://host.image"));
        CompanionPostEntity post = postRepository.saveAndFlush(post(
                postPlace.getId(), CompanionPostMeetingTimeType.SCHEDULED,
                CREATED_AT.plusDays(1), null, 5, "https://open-chat.latest"));
        CompanionApplicationEntity application = applicationRepository.saveAndFlush(application(
                post.getId(), REQUESTER_USER_ID, CompanionApplicationStatus.ACCEPTED));
        CompanionMatchEntity match = matchRepository.saveAndFlush(match(post.getId(), CompanionMatchStatus.SCHEDULE_CONFIRMED));
        participantRepository.saveAndFlush(participant(match.getId(), HOST_USER_ID, null, MatchParticipantRole.HOST));
        participantRepository.saveAndFlush(participant(
                match.getId(), REQUESTER_USER_ID, application.getId(), MatchParticipantRole.GUEST));
        participantRepository.saveAndFlush(participant(match.getId(), 9L, null, MatchParticipantRole.GUEST));
        LocalDateTime confirmedAt = CREATED_AT.plusDays(2);
        scheduleRepository.saveAndFlush(new CompanionScheduleEntity(
                null, match.getId(), confirmedPlace.getId(), confirmedAt, 90, true));

        AcceptedCompanionRequestDetailQueryAdapter adapter = new AcceptedCompanionRequestDetailQueryAdapter(queryRepository);

        Optional<AcceptedCompanionRequestDetail> result = adapter.findByApplicationIdAndRequesterUserId(
                application.getId(), REQUESTER_USER_ID);

        assertThat(result).isPresent();
        AcceptedCompanionRequestDetail detail = result.get();
        assertThat(detail.matchId()).isEqualTo(match.getId());
        assertThat(detail.matchStatus()).isEqualTo(CompanionMatchStatus.SCHEDULE_CONFIRMED);
        assertThat(detail.postId()).isEqualTo(post.getId());
        assertThat(detail.host()).isEqualTo(new AcceptedCompanionRequestDetail.Host(
                HOST_USER_ID, "호스트", "https://host.image"));
        assertThat(detail.place()).isEqualTo(new AcceptedCompanionRequestDetail.Place(
                "confirmed-place", "확정 장소", "확정 주소",
                new BigDecimal("37.20000000"), new BigDecimal("127.20000000")));
        assertThat(detail.meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.SCHEDULED);
        assertThat(detail.meetingAt()).isEqualTo(confirmedAt);
        assertThat(detail.participantCount()).isEqualTo(3);
        assertThat(detail.maxParticipants()).isEqualTo(5);
        assertThat(detail.openChatUrl()).isEqualTo("https://open-chat.latest");
        assertThat(adapter.findByApplicationIdAndRequesterUserId(application.getId(), 99L)).isEmpty();
    }

    @Test
    void selectsConfirmedPlaceAsAWholeWhenItsAddressIsNull() {
        PlaceCacheEntity postPlace = placeRepository.saveAndFlush(place(
                "post-place", "모집 장소", "모집 주소", "37.10000000", "127.10000000"));
        PlaceCacheEntity confirmedPlace = placeRepository.saveAndFlush(place(
                "confirmed-place", "확정 장소", null, "37.20000000", "127.20000000"));
        profileRepository.saveAndFlush(profile(HOST_USER_ID, "호스트", "https://host.image"));
        CompanionPostEntity post = postRepository.saveAndFlush(post(
                postPlace.getId(), CompanionPostMeetingTimeType.SCHEDULED,
                CREATED_AT.plusDays(1), null, 4, "https://confirmed-chat"));
        CompanionApplicationEntity application = applicationRepository.saveAndFlush(application(
                post.getId(), REQUESTER_USER_ID, CompanionApplicationStatus.ACCEPTED));
        CompanionMatchEntity match = matchRepository.saveAndFlush(match(post.getId(), CompanionMatchStatus.MATCHED));
        participantRepository.saveAndFlush(participant(match.getId(), HOST_USER_ID, null, MatchParticipantRole.HOST));
        participantRepository.saveAndFlush(participant(
                match.getId(), REQUESTER_USER_ID, application.getId(), MatchParticipantRole.GUEST));
        scheduleRepository.saveAndFlush(new CompanionScheduleEntity(
                null, match.getId(), confirmedPlace.getId(), CREATED_AT.plusDays(2), 90, true));

        AcceptedCompanionRequestDetail.Place result = adapter()
                .findByApplicationIdAndRequesterUserId(application.getId(), REQUESTER_USER_ID)
                .orElseThrow()
                .place();

        assertThat(result).isEqualTo(new AcceptedCompanionRequestDetail.Place(
                "confirmed-place", "확정 장소", null,
                new BigDecimal("37.20000000"), new BigDecimal("127.20000000")));
    }

    @Test
    void returnsNewestGuestParticipantMatchWhenApplicationIsLinkedToMultipleMatches() {
        PlaceCacheEntity postPlace = placeRepository.saveAndFlush(place(
                "post-place", "모집 장소", "모집 주소", "37.10000000", "127.10000000"));
        profileRepository.saveAndFlush(profile(HOST_USER_ID, "호스트", "https://host.image"));
        CompanionPostEntity post = postRepository.saveAndFlush(post(
                postPlace.getId(), CompanionPostMeetingTimeType.SCHEDULED,
                CREATED_AT.plusDays(1), null, 4, "https://multiple-match-chat"));
        CompanionApplicationEntity application = applicationRepository.saveAndFlush(application(
                post.getId(), REQUESTER_USER_ID, CompanionApplicationStatus.ACCEPTED));
        CompanionMatchEntity olderMatch = matchRepository.saveAndFlush(
                match(post.getId(), CompanionMatchStatus.MATCHED));
        participantRepository.saveAndFlush(participant(
                olderMatch.getId(), HOST_USER_ID, null, MatchParticipantRole.HOST));
        participantRepository.saveAndFlush(participant(
                olderMatch.getId(), REQUESTER_USER_ID, application.getId(), MatchParticipantRole.GUEST));
        CompanionMatchEntity newestMatch = matchRepository.saveAndFlush(
                match(post.getId(), CompanionMatchStatus.SCHEDULE_CONFIRMED));
        participantRepository.saveAndFlush(participant(
                newestMatch.getId(), HOST_USER_ID, null, MatchParticipantRole.HOST));
        participantRepository.saveAndFlush(participant(
                newestMatch.getId(), REQUESTER_USER_ID, application.getId(), MatchParticipantRole.GUEST));

        Optional<AcceptedCompanionRequestDetail> result = adapter().findByApplicationIdAndRequesterUserId(
                application.getId(), REQUESTER_USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().matchId()).isEqualTo(newestMatch.getId());
        assertThat(result.get().matchStatus()).isEqualTo(CompanionMatchStatus.SCHEDULE_CONFIRMED);
    }

    @Test
    void fallsBackToPostPlaceAndScheduledMeetingTimeWithoutConfirmedSchedule() {
        Fixture fixture = saveAcceptedFixture(
                CompanionPostMeetingTimeType.SCHEDULED,
                CREATED_AT.plusDays(1),
                null,
                "https://scheduled-chat"
        );

        Optional<AcceptedCompanionRequestDetail> result = adapter().findByApplicationIdAndRequesterUserId(
                fixture.applicationId(), REQUESTER_USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().place().googlePlaceId()).isEqualTo("post-place");
        assertThat(result.get().meetingAt()).isEqualTo(CREATED_AT.plusDays(1));
    }

    @Test
    void returnsNullMeetingTimeForUndecidedPostWithoutConfirmedSchedule() {
        Fixture fixture = saveAcceptedFixture(
                CompanionPostMeetingTimeType.UNDECIDED,
                null,
                null,
                "https://undecided-chat"
        );

        Optional<AcceptedCompanionRequestDetail> result = adapter().findByApplicationIdAndRequesterUserId(
                fixture.applicationId(), REQUESTER_USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.UNDECIDED);
        assertThat(result.get().meetingAt()).isNull();
    }

    @Test
    void fallsBackToExposureExpirationForNowPostWithoutConfirmedSchedule() {
        LocalDateTime exposureExpiresAt = CREATED_AT.plusHours(2);
        Fixture fixture = saveAcceptedFixture(
                CompanionPostMeetingTimeType.NOW,
                null,
                exposureExpiresAt,
                "https://now-chat"
        );

        Optional<AcceptedCompanionRequestDetail> result = adapter().findByApplicationIdAndRequesterUserId(
                fixture.applicationId(), REQUESTER_USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().meetingTimeType()).isEqualTo(CompanionPostMeetingTimeType.NOW);
        assertThat(result.get().meetingAt()).isEqualTo(exposureExpiresAt);
    }

    private Fixture saveAcceptedFixture(
            final CompanionPostMeetingTimeType timeType,
            final LocalDateTime meetingAt,
            final LocalDateTime exposureExpiresAt,
            final String openChatUrl
    ) {
        PlaceCacheEntity postPlace = placeRepository.saveAndFlush(place(
                "post-place", "모집 장소", "모집 주소", "37.10000000", "127.10000000"));
        profileRepository.saveAndFlush(profile(HOST_USER_ID, "호스트", "https://host.image"));
        CompanionPostEntity post = postRepository.saveAndFlush(post(
                postPlace.getId(), timeType, meetingAt, exposureExpiresAt, 4, openChatUrl));
        CompanionApplicationEntity application = applicationRepository.saveAndFlush(application(
                post.getId(), REQUESTER_USER_ID, CompanionApplicationStatus.ACCEPTED));
        CompanionMatchEntity match = matchRepository.saveAndFlush(match(post.getId(), CompanionMatchStatus.MATCHED));
        participantRepository.saveAndFlush(participant(match.getId(), HOST_USER_ID, null, MatchParticipantRole.HOST));
        participantRepository.saveAndFlush(participant(
                match.getId(), REQUESTER_USER_ID, application.getId(), MatchParticipantRole.GUEST));
        return new Fixture(application.getId());
    }

    private AcceptedCompanionRequestDetailQueryAdapter adapter() {
        return new AcceptedCompanionRequestDetailQueryAdapter(queryRepository);
    }

    private PlaceCacheEntity place(
            final String googlePlaceId,
            final String name,
            final String address,
            final String latitude,
            final String longitude
    ) {
        return new PlaceCacheEntity(
                null, googlePlaceId, name, address,
                new BigDecimal(latitude), new BigDecimal(longitude),
                "restaurant", null, new BigDecimal("4.50"), 10, null,
                PlaceBusinessStatus.OPERATIONAL
        );
    }

    private CompanionProfileEntity profile(final Long userId, final String nickname, final String imageUrl) {
        return new CompanionProfileEntity(
                null, userId, nickname, UserGender.FEMALE, 1998, imageUrl,
                "소개", new BigDecimal("4.50"), 3, CompanionProfileStatus.ACTIVE
        );
    }

    private CompanionPostEntity post(
            final Long placeId,
            final CompanionPostMeetingTimeType timeType,
            final LocalDateTime meetingAt,
            final LocalDateTime exposureExpiresAt,
            final int maxParticipants,
            final String openChatUrl
    ) {
        return new CompanionPostEntity(
                null, HOST_USER_ID, placeId, timeType, meetingAt, exposureExpiresAt,
                maxParticipants, true, "동행 모집", openChatUrl, CompanionPostStatus.CLOSED, CREATED_AT
        );
    }

    private CompanionApplicationEntity application(
            final Long postId,
            final Long applicantUserId,
            final CompanionApplicationStatus status
    ) {
        return new CompanionApplicationEntity(null, postId, applicantUserId, status, null, CREATED_AT);
    }

    private CompanionMatchEntity match(final Long postId, final CompanionMatchStatus status) {
        return new CompanionMatchEntity(null, postId, status, CREATED_AT);
    }

    private CompanionMatchParticipantEntity participant(
            final Long matchId,
            final Long userId,
            final Long acceptedApplicationId,
            final MatchParticipantRole role
    ) {
        return new CompanionMatchParticipantEntity(null, matchId, userId, acceptedApplicationId, role);
    }

    private record Fixture(Long applicationId) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionApplicationEntity.class,
            CompanionPostEntity.class,
            CompanionMatchEntity.class,
            CompanionMatchParticipantEntity.class,
            CompanionScheduleEntity.class,
            CompanionProfileEntity.class,
            PlaceCacheEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionApplicationJpaRepository.class,
            CompanionPostJpaRepository.class,
            CompanionMatchJpaRepository.class,
            CompanionMatchParticipantJpaRepository.class,
            CompanionScheduleJpaRepository.class,
            CompanionProfileJpaRepository.class,
            PlaceCacheJpaRepository.class,
            AcceptedCompanionRequestDetailJpaRepository.class
    })
    static class TestApplication {
    }
}
