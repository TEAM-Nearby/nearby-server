// 동행 알림 목록 조회 어댑터의 조인 쿼리와 매핑을 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchParticipantEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionNotificationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionApplicationJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchParticipantJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionNotificationQueryJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationActionType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class CompanionNotificationQueryAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 4, 12, 0);

    @Autowired
    private CompanionApplicationJpaRepository applicationJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CompanionMatchJpaRepository matchJpaRepository;

    @Autowired
    private CompanionMatchParticipantJpaRepository participantJpaRepository;

    @Autowired
    private CompanionNotificationQueryJpaRepository notificationJpaRepository;

    @Autowired
    private CompanionPostJpaRepository postJpaRepository;

    @Autowired
    private CompanionProfileJpaRepository profileJpaRepository;

    @Autowired
    private CompanionScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Test
    void findsSentNotificationsWithHostProfileReadStatusMatchIdAndLatestOrder() {
        CompanionNotificationQueryAdapter adapter = new CompanionNotificationQueryAdapter(notificationJpaRepository);

        CompanionProfileEntity hostA = profileJpaRepository.saveAndFlush(profile(100L, "호스트A", "host-a.png"));
        profileJpaRepository.saveAndFlush(profile(200L, "호스트B", null));

        PlaceCacheEntity placeA = placeCacheJpaRepository.saveAndFlush(place("google-place-a", "오노테라"));
        PlaceCacheEntity placeB = placeCacheJpaRepository.saveAndFlush(place("google-place-b", "시우다드 콘달"));
        PlaceCacheEntity schedulePlace = placeCacheJpaRepository.saveAndFlush(place(
                "google-schedule-place",
                "확정 장소"
        ));

        CompanionPostEntity recentPost = postJpaRepository.saveAndFlush(nowPost(
                hostA.getUserId(),
                placeA.getId(),
                NOW.plusHours(1)
        ));
        CompanionApplicationEntity recentApplication = applicationJpaRepository.saveAndFlush(application(
                recentPost.getId(),
                7L,
                CompanionApplicationStatus.ACCEPTED,
                NOW.plusHours(2)
        ));
        CompanionMatchEntity match = matchJpaRepository.saveAndFlush(match(recentPost.getId()));
        LocalDateTime scheduledAt = NOW.plusMinutes(20);
        scheduleJpaRepository.saveAndFlush(schedule(match.getId(), schedulePlace.getId(), scheduledAt));
        participantJpaRepository.saveAndFlush(participant(
                match.getId(),
                7L,
                recentApplication.getId()
        ));
        CompanionNotificationEntity recentNotification = entityManager.persistAndFlush(notification(
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                recentApplication.getId(),
                NOW.plusHours(4),
                NOW.plusHours(3)
        ));

        CompanionPostEntity oldPost = postJpaRepository.saveAndFlush(post(
                200L,
                placeB.getId(),
                NOW.plusDays(2)
        ));
        CompanionApplicationEntity oldApplication = applicationJpaRepository.saveAndFlush(application(
                oldPost.getId(),
                7L,
                CompanionApplicationStatus.REJECTED,
                NOW.minusHours(1)
        ));
        CompanionNotificationEntity oldNotification = entityManager.persistAndFlush(notification(
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_REJECTED,
                oldApplication.getId(),
                null,
                NOW.plusHours(1)
        ));

        CompanionApplicationEntity otherUserApplication = applicationJpaRepository.saveAndFlush(application(
                recentPost.getId(),
                999L,
                CompanionApplicationStatus.ACCEPTED,
                NOW.plusHours(3)
        ));
        entityManager.persistAndFlush(notification(
                999L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                otherUserApplication.getId(),
                null,
                NOW.plusHours(5)
        ));
        CompanionNotificationEntity mismatchedRecipientNotification = entityManager.persistAndFlush(notification(
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                otherUserApplication.getId(),
                null,
                NOW.plusHours(6)
        ));

        List<CompanionNotificationSummary> result = adapter.findAllByUserIdAndDirection(
                7L,
                CompanionNotificationDirection.SENT
        );

        assertThat(result)
                .extracting(CompanionNotificationSummary::notificationId)
                .containsExactly(recentNotification.getId(), oldNotification.getId())
                .doesNotContain(mismatchedRecipientNotification.getId());

        CompanionNotificationSummary first = result.get(0);
        assertThat(first.notificationId()).isEqualTo(recentNotification.getId());
        assertThat(first.applicationId()).isEqualTo(recentApplication.getId());
        assertThat(first.applicationStatus()).isEqualTo(CompanionApplicationStatus.ACCEPTED);
        assertThat(first.host().userId()).isEqualTo(100L);
        assertThat(first.host().profileImageUrl()).isEqualTo("host-a.png");
        assertThat(first.host().nickname()).isEqualTo("호스트A");
        assertThat(first.placeName()).isEqualTo("확정 장소");
        assertThat(first.meetingAt()).isEqualTo(scheduledAt);
        assertThat(first.matchId()).isEqualTo(match.getId());
        assertThat(first.actionType()).isEqualTo(CompanionNotificationActionType.VIEW_RESULT);
        assertThat(first.isRead()).isTrue();

        CompanionNotificationSummary second = result.get(1);
        assertThat(second.notificationId()).isEqualTo(oldNotification.getId());
        assertThat(second.applicationId()).isEqualTo(oldApplication.getId());
        assertThat(second.applicationStatus()).isEqualTo(CompanionApplicationStatus.REJECTED);
        assertThat(second.host().userId()).isEqualTo(200L);
        assertThat(second.host().profileImageUrl()).isNull();
        assertThat(second.host().nickname()).isEqualTo("호스트B");
        assertThat(second.placeName()).isEqualTo("시우다드 콘달");
        assertThat(second.matchId()).isNull();
        assertThat(second.actionType()).isEqualTo(CompanionNotificationActionType.VIEW_RESULT);
        assertThat(second.isRead()).isFalse();
    }

    @Test
    void findsReceivedNotificationsForHostAndMapsActionTypes() {
        CompanionNotificationQueryAdapter adapter = new CompanionNotificationQueryAdapter(notificationJpaRepository);

        profileJpaRepository.saveAndFlush(profile(100L, "호스트", "host.png"));
        profileJpaRepository.saveAndFlush(profile(200L, "다른호스트", "other-host.png"));

        PlaceCacheEntity placeA = placeCacheJpaRepository.saveAndFlush(place("google-place-a", "오노테라"));
        PlaceCacheEntity placeB = placeCacheJpaRepository.saveAndFlush(place("google-place-b", "BRAMS"));
        PlaceCacheEntity placeC = placeCacheJpaRepository.saveAndFlush(place("google-place-c", "시우다드 콘달"));
        PlaceCacheEntity schedulePlace = placeCacheJpaRepository.saveAndFlush(place(
                "google-schedule-place",
                "확정 장소"
        ));

        CompanionPostEntity recentPost = postJpaRepository.saveAndFlush(post(
                100L,
                placeA.getId(),
                NOW.plusDays(1)
        ));
        CompanionApplicationEntity pendingApplication = applicationJpaRepository.saveAndFlush(application(
                recentPost.getId(),
                7L,
                CompanionApplicationStatus.PENDING,
                NOW.plusHours(3)
        ));
        CompanionNotificationEntity pendingNotification = entityManager.persistAndFlush(notification(
                100L,
                CompanionNotificationType.COMPANION_APPLICATION_CREATED,
                pendingApplication.getId(),
                null,
                NOW.plusHours(3)
        ));

        CompanionPostEntity acceptedPost = postJpaRepository.saveAndFlush(post(
                100L,
                placeB.getId(),
                NOW.plusDays(2)
        ));
        CompanionApplicationEntity acceptedApplication = applicationJpaRepository.saveAndFlush(application(
                acceptedPost.getId(),
                8L,
                CompanionApplicationStatus.ACCEPTED,
                NOW.plusHours(2)
        ));
        CompanionMatchEntity match = matchJpaRepository.saveAndFlush(match(acceptedPost.getId()));
        scheduleJpaRepository.saveAndFlush(schedule(match.getId(), schedulePlace.getId(), NOW.plusHours(1)));
        participantJpaRepository.saveAndFlush(participant(
                match.getId(),
                8L,
                acceptedApplication.getId()
        ));
        CompanionNotificationEntity acceptedNotification = entityManager.persistAndFlush(notification(
                100L,
                CompanionNotificationType.COMPANION_APPLICATION_CREATED,
                acceptedApplication.getId(),
                NOW.plusHours(4),
                NOW.plusHours(2)
        ));

        CompanionPostEntity rejectedPost = postJpaRepository.saveAndFlush(post(
                100L,
                placeC.getId(),
                NOW.plusDays(3)
        ));
        CompanionApplicationEntity rejectedApplication = applicationJpaRepository.saveAndFlush(application(
                rejectedPost.getId(),
                9L,
                CompanionApplicationStatus.REJECTED,
                NOW.plusHours(1)
        ));
        CompanionNotificationEntity rejectedNotification = entityManager.persistAndFlush(notification(
                100L,
                CompanionNotificationType.COMPANION_APPLICATION_CREATED,
                rejectedApplication.getId(),
                null,
                NOW.plusHours(1)
        ));

        CompanionPostEntity otherHostPost = postJpaRepository.saveAndFlush(post(
                200L,
                placeA.getId(),
                NOW.plusDays(4)
        ));
        CompanionApplicationEntity otherHostApplication = applicationJpaRepository.saveAndFlush(application(
                otherHostPost.getId(),
                7L,
                CompanionApplicationStatus.PENDING,
                NOW.plusHours(4)
        ));
        entityManager.persistAndFlush(notification(
                200L,
                CompanionNotificationType.COMPANION_APPLICATION_CREATED,
                otherHostApplication.getId(),
                null,
                NOW.plusHours(5)
        ));
        CompanionNotificationEntity mismatchedRecipientNotification = entityManager.persistAndFlush(notification(
                100L,
                CompanionNotificationType.COMPANION_APPLICATION_CREATED,
                otherHostApplication.getId(),
                null,
                NOW.plusHours(6)
        ));

        List<CompanionNotificationSummary> result = adapter.findAllByUserIdAndDirection(
                100L,
                CompanionNotificationDirection.RECEIVED
        );

        assertThat(result)
                .extracting(CompanionNotificationSummary::notificationId)
                .containsExactly(
                        pendingNotification.getId(),
                        acceptedNotification.getId(),
                        rejectedNotification.getId()
                )
                .doesNotContain(mismatchedRecipientNotification.getId());
        assertThat(result.get(0).notificationId()).isEqualTo(pendingNotification.getId());
        assertThat(result.get(0).applicationId()).isEqualTo(pendingApplication.getId());
        assertThat(result.get(0).actionType()).isEqualTo(CompanionNotificationActionType.ACCEPT_REQUEST);
        assertThat(result.get(0).isRead()).isFalse();

        assertThat(result.get(1).notificationId()).isEqualTo(acceptedNotification.getId());
        assertThat(result.get(1).applicationId()).isEqualTo(acceptedApplication.getId());
        assertThat(result.get(1).matchId()).isEqualTo(match.getId());
        assertThat(result.get(1).placeName()).isEqualTo("확정 장소");
        assertThat(result.get(1).actionType()).isEqualTo(CompanionNotificationActionType.CONFIRM_SCHEDULE);
        assertThat(result.get(1).isRead()).isTrue();

        assertThat(result.get(2).notificationId()).isEqualTo(rejectedNotification.getId());
        assertThat(result.get(2).applicationId()).isEqualTo(rejectedApplication.getId());
        assertThat(result.get(2).actionType()).isEqualTo(CompanionNotificationActionType.NONE);
        assertThat(result.get(2).isRead()).isFalse();
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

    private CompanionProfileEntity profile(
            final Long userId,
            final String nickname,
            final String profileImageUrl
    ) {
        return new CompanionProfileEntity(
                null,
                userId,
                nickname,
                UserGender.FEMALE,
                2000,
                profileImageUrl,
                "반가워요.",
                new BigDecimal("4.50"),
                0,
                CompanionProfileStatus.ACTIVE
        );
    }

    private CompanionPostEntity post(
            final Long hostUserId,
            final Long placeId,
            final LocalDateTime meetingAt
    ) {
        return new CompanionPostEntity(
                null,
                hostUserId,
                placeId,
                meetingAt,
                4,
                "같이 밥 먹어요.",
                "https://openchat.example",
                CompanionPostStatus.CLOSED,
                NOW
        );
    }

    private CompanionPostEntity nowPost(
            final Long hostUserId,
            final Long placeId,
            final LocalDateTime exposureExpiresAt
    ) {
        return new CompanionPostEntity(
                null,
                hostUserId,
                placeId,
                CompanionPostMeetingTimeType.NOW,
                null,
                exposureExpiresAt,
                4,
                true,
                "지금 같이 밥 먹어요.",
                "https://openchat.example",
                CompanionPostStatus.CLOSED,
                NOW
        );
    }

    private CompanionApplicationEntity application(
            final Long postId,
            final Long applicantUserId,
            final CompanionApplicationStatus status,
            final LocalDateTime createdAt
    ) {
        return new CompanionApplicationEntity(
                null,
                postId,
                applicantUserId,
                status,
                null,
                createdAt
        );
    }

    private CompanionMatchEntity match(final Long postId) {
        return new CompanionMatchEntity(
                null,
                postId,
                CompanionMatchStatus.MATCHED,
                NOW
        );
    }

    private CompanionMatchParticipantEntity participant(
            final Long matchId,
            final Long userId,
            final Long acceptedApplicationId
    ) {
        return new CompanionMatchParticipantEntity(
                null,
                matchId,
                userId,
                acceptedApplicationId,
                MatchParticipantRole.GUEST
        );
    }

    private CompanionScheduleEntity schedule(
            final Long matchId,
            final Long placeId,
            final LocalDateTime scheduledAt
    ) {
        return new CompanionScheduleEntity(null, matchId, placeId, scheduledAt, 120, true);
    }

    private CompanionNotificationEntity notification(
            final Long recipientUserId,
            final CompanionNotificationType notificationType,
            final Long applicationId,
            final LocalDateTime readAt,
            final LocalDateTime createdAt
    ) {
        return new CompanionNotificationEntity(
                null,
                recipientUserId,
                notificationType,
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                applicationId,
                readAt,
                createdAt
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            CompanionApplicationEntity.class,
            CompanionMatchEntity.class,
            CompanionMatchParticipantEntity.class,
            CompanionNotificationEntity.class,
            CompanionPostEntity.class,
            CompanionProfileEntity.class,
            CompanionScheduleEntity.class,
            PlaceCacheEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            CompanionApplicationJpaRepository.class,
            CompanionMatchJpaRepository.class,
            CompanionMatchParticipantJpaRepository.class,
            CompanionNotificationQueryJpaRepository.class,
            CompanionPostJpaRepository.class,
            CompanionProfileJpaRepository.class,
            CompanionScheduleJpaRepository.class,
            PlaceCacheJpaRepository.class
    })
    static class TestApplication {
    }
}
