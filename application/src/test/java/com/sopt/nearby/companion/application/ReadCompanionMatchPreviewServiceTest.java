// 동행 매칭 미리보기 조회 서비스의 권한 검증과 응답 조립을 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionPostNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionMatchException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMatchIdException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionSchedule;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadCompanionMatchPreviewServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 4, 12, 0);

    private FakeCompanionMatchRepository companionMatchRepository;
    private FakeCompanionPostRepository companionPostRepository;
    private FakeCompanionMatchParticipantRepository companionMatchParticipantRepository;
    private FakeCompanionProfileRepository companionProfileRepository;
    private FakeCompanionScheduleRepository companionScheduleRepository;
    private ReadCompanionMatchPreviewService service;

    @BeforeEach
    void setUp() {
        companionMatchRepository = new FakeCompanionMatchRepository();
        companionPostRepository = new FakeCompanionPostRepository();
        companionMatchParticipantRepository = new FakeCompanionMatchParticipantRepository();
        companionProfileRepository = new FakeCompanionProfileRepository();
        companionScheduleRepository = new FakeCompanionScheduleRepository();
        service = new ReadCompanionMatchPreviewService(
                companionMatchRepository,
                companionPostRepository,
                companionMatchParticipantRepository,
                companionProfileRepository,
                companionScheduleRepository
        );
    }

    @Test
    void returnsPreviewWhenRequesterIsMatchParticipant() {
        saveDefaultMatch();
        saveDefaultParticipantsAndProfiles();

        CompanionMatchPreview preview = service.getPreview(10L, 7L);

        assertEquals(10L, preview.matchId());
        assertEquals(2, preview.members().size());
        assertEquals(7L, preview.members().get(0).userId());
        assertEquals("https://image.example/a.png", preview.members().get(0).profileImageUrl());
        assertEquals("여행자A", preview.members().get(0).nickname());
        assertEquals(8L, preview.members().get(1).userId());
        assertNull(preview.members().get(1).profileImageUrl());
        assertEquals("여행자B", preview.members().get(1).nickname());
        assertEquals(20L, preview.companionPost().postId());
        assertEquals("함께 밥 먹을 동행을 구해요.", preview.companionPost().content());
        assertEquals(CompanionPostMeetingTimeType.SCHEDULED, preview.companionPost().meetingTimeType());
        assertEquals(NOW.plusDays(1), preview.companionPost().meetingAt());
    }

    @Test
    void returnsConfirmedScheduleMeetingAtBeforePostMeetingAt() {
        saveDefaultMatch();
        saveDefaultParticipantsAndProfiles();
        LocalDateTime confirmedScheduleAt = NOW.plusHours(3);
        companionScheduleRepository.save(new CompanionSchedule(
                1L,
                10L,
                30L,
                confirmedScheduleAt,
                90,
                true
        ));

        CompanionMatchPreview preview = service.getPreview(10L, 7L);

        assertEquals(CompanionPostMeetingTimeType.SCHEDULED, preview.companionPost().meetingTimeType());
        assertEquals(confirmedScheduleAt, preview.companionPost().meetingAt());
    }

    @Test
    void returnsExposureExpiresAtAsMeetingAtForNowPostWithoutConfirmedSchedule() {
        LocalDateTime exposureExpiresAt = NOW.plusHours(2);
        saveMatchWithPost(new CompanionPost(
                20L,
                7L,
                30L,
                CompanionPostMeetingTimeType.NOW,
                null,
                exposureExpiresAt,
                4,
                true,
                "함께 밥 먹을 동행을 구해요.",
                "https://openchat.example",
                CompanionPostStatus.CLOSED,
                NOW
        ));
        saveDefaultParticipantsAndProfiles();

        CompanionMatchPreview preview = service.getPreview(10L, 7L);

        assertEquals(CompanionPostMeetingTimeType.NOW, preview.companionPost().meetingTimeType());
        assertEquals(exposureExpiresAt, preview.companionPost().meetingAt());
    }

    @Test
    void returnsNullMeetingAtForUndecidedPostWithoutConfirmedSchedule() {
        saveMatchWithPost(new CompanionPost(
                20L,
                7L,
                30L,
                CompanionPostMeetingTimeType.UNDECIDED,
                null,
                null,
                4,
                true,
                "함께 밥 먹을 동행을 구해요.",
                "https://openchat.example",
                CompanionPostStatus.CLOSED,
                NOW
        ));
        saveDefaultParticipantsAndProfiles();

        CompanionMatchPreview preview = service.getPreview(10L, 7L);

        assertEquals(CompanionPostMeetingTimeType.UNDECIDED, preview.companionPost().meetingTimeType());
        assertNull(preview.companionPost().meetingAt());
    }

    @Test
    void throwsNotFoundWhenMatchDoesNotExist() {
        assertThrows(
                CompanionMatchNotFoundException.class,
                () -> service.getPreview(999L, 7L)
        );
    }

    @Test
    void throwsForbiddenWhenRequesterIsNotMatchParticipant() {
        saveDefaultMatch();
        companionMatchParticipantRepository.save(new CompanionMatchParticipant(
                1L,
                10L,
                8L,
                null,
                MatchParticipantRole.GUEST
        ));

        assertThrows(
                ForbiddenCompanionMatchException.class,
                () -> service.getPreview(10L, 7L)
        );
    }

    @Test
    void throwsInvalidMatchIdWhenMatchIdIsNotPositive() {
        assertThrows(
                InvalidCompanionMatchIdException.class,
                () -> service.getPreview(0L, 7L)
        );
    }

    @Test
    void throwsPostNotFoundWhenMatchedPostDoesNotExist() {
        companionMatchRepository.save(new CompanionMatch(
                10L,
                20L,
                CompanionMatchStatus.MATCHED,
                NOW
        ));
        companionMatchParticipantRepository.save(new CompanionMatchParticipant(
                1L,
                10L,
                7L,
                null,
                MatchParticipantRole.HOST
        ));

        assertThrows(
                CompanionPostNotFoundException.class,
                () -> service.getPreview(10L, 7L)
        );
    }

    private void saveDefaultMatch() {
        saveMatchWithPost(new CompanionPost(
                20L,
                7L,
                30L,
                NOW.plusDays(1),
                4,
                "함께 밥 먹을 동행을 구해요.",
                "https://openchat.example",
                CompanionPostStatus.CLOSED,
                NOW
        ));
    }

    private void saveMatchWithPost(final CompanionPost post) {
        companionMatchRepository.save(new CompanionMatch(
                10L,
                post.id(),
                CompanionMatchStatus.MATCHED,
                NOW
        ));
        companionPostRepository.save(post);
    }

    private void saveDefaultParticipantsAndProfiles() {
        companionMatchParticipantRepository.save(new CompanionMatchParticipant(
                1L,
                10L,
                7L,
                null,
                MatchParticipantRole.HOST
        ));
        companionMatchParticipantRepository.save(new CompanionMatchParticipant(
                2L,
                10L,
                8L,
                null,
                MatchParticipantRole.GUEST
        ));
        companionProfileRepository.save(profile(1L, 7L, "여행자A", "https://image.example/a.png"));
        companionProfileRepository.save(profile(2L, 8L, "여행자B", null));
    }

    private CompanionProfile profile(
            final Long id,
            final Long userId,
            final String nickname,
            final String profileImageUrl
    ) {
        return new CompanionProfile(
                id,
                userId,
                nickname,
                UserGender.FEMALE,
                2000,
                profileImageUrl,
                "반가워요.",
                BigDecimal.valueOf(36.5),
                0,
                CompanionProfileStatus.ACTIVE
        );
    }

    private static final class FakeCompanionMatchRepository implements CompanionMatchRepository {

        private final Map<Long, CompanionMatch> matches = new HashMap<>();

        @Override
        public CompanionMatch save(final CompanionMatch model) {
            matches.put(model.id(), model);
            return model;
        }

        @Override
        public Optional<CompanionMatch> findById(final Long id) {
            return Optional.ofNullable(matches.get(id));
        }

        @Override
        public Optional<CompanionMatch> findFirstByPostIdAndStatus(
                final Long postId,
                final CompanionMatchStatus status
        ) {
            return Optional.empty();
        }

        @Override
        public boolean confirmScheduleIfMatched(final Long matchId) {
            return false;
        }
    }

    private static final class FakeCompanionPostRepository implements CompanionPostRepository {

        private final Map<Long, CompanionPost> posts = new HashMap<>();

        @Override
        public CompanionPost save(final CompanionPost model) {
            posts.put(model.id(), model);
            return model;
        }

        @Override
        public Optional<CompanionPost> findById(final Long id) {
            return Optional.ofNullable(posts.get(id));
        }
    }

    private static final class FakeCompanionMatchParticipantRepository implements CompanionMatchParticipantRepository {

        private final List<CompanionMatchParticipant> participants = new ArrayList<>();

        @Override
        public CompanionMatchParticipant save(final CompanionMatchParticipant model) {
            participants.add(model);
            return model;
        }

        @Override
        public Optional<CompanionMatchParticipant> findById(final Long id) {
            return participants.stream()
                    .filter(participant -> participant.id().equals(id))
                    .findFirst();
        }

        @Override
        public List<CompanionMatchParticipant> findAllByMatchId(final Long matchId) {
            return participants.stream()
                    .filter(participant -> participant.matchId().equals(matchId))
                    .toList();
        }

        @Override
        public boolean existsByMatchIdAndUserId(final Long matchId, final Long userId) {
            return participants.stream()
                    .anyMatch(participant -> participant.matchId().equals(matchId)
                            && participant.userId().equals(userId));
        }
    }

    private static final class FakeCompanionProfileRepository implements CompanionProfileRepository {

        private final Map<Long, CompanionProfile> profiles = new HashMap<>();

        @Override
        public CompanionProfile save(final CompanionProfile model) {
            profiles.put(model.id(), model);
            return model;
        }

        @Override
        public Optional<CompanionProfile> findById(final Long id) {
            return Optional.ofNullable(profiles.get(id));
        }

        @Override
        public List<CompanionProfile> findAllByUserIdIn(final List<Long> userIds) {
            return profiles.values()
                    .stream()
                    .filter(profile -> userIds.contains(profile.userId()))
                    .toList();
        }

        @Override
        public boolean existsByNickname(final String nickname) {
            return profiles.values()
                    .stream()
                    .anyMatch(profile -> profile.nickname().equals(nickname));
        }

        @Override
        public boolean existsByUserId(final Long userId) {
            return profiles.values()
                    .stream()
                    .anyMatch(profile -> profile.userId().equals(userId));
        }

        @Override
        public Optional<CompanionProfile> findByUserId(final Long userId) {
            return profiles.values()
                    .stream()
                    .filter(profile -> profile.userId().equals(userId))
                    .findFirst();
        }
    }

    private static final class FakeCompanionScheduleRepository implements CompanionScheduleRepository {

        private final Map<Long, CompanionSchedule> schedules = new HashMap<>();

        @Override
        public CompanionSchedule save(final CompanionSchedule model) {
            schedules.put(model.id(), model);
            return model;
        }

        @Override
        public Optional<CompanionSchedule> findById(final Long id) {
            return Optional.ofNullable(schedules.get(id));
        }

        @Override
        public Optional<CompanionSchedule> findConfirmedByMatchId(final Long matchId) {
            return schedules.values()
                    .stream()
                    .filter(schedule -> schedule.matchId().equals(matchId))
                    .filter(CompanionSchedule::confirmed)
                    .findFirst();
        }
    }
}
