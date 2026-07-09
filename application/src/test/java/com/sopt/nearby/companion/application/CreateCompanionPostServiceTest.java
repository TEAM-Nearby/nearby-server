// 동행 모집 글 작성 유스케이스의 저장 규칙과 검증을 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionPostCreateRequestException;
import com.sopt.nearby.companion.domain.exception.InvalidOpenChatUrlException;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostKeyword;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStyle;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionPostStyleRepository;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceCache;
import com.sopt.nearby.user.exception.OnboardingRequiredException;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateCompanionPostServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-02T05:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 2, 5, 0);

    private FakeRequireCompletedOnboardingUseCase onboardingUseCase;
    private FakeResolvePlaceCacheUseCase resolvePlaceCacheUseCase;
    private FakeCompanionPostRepository postRepository;
    private FakeCompanionPostStyleRepository styleRepository;
    private CreateCompanionPostService service;

    @BeforeEach
    void setUp() {
        onboardingUseCase = new FakeRequireCompletedOnboardingUseCase();
        resolvePlaceCacheUseCase = new FakeResolvePlaceCacheUseCase();
        postRepository = new FakeCompanionPostRepository();
        styleRepository = new FakeCompanionPostStyleRepository();
        service = new CreateCompanionPostService(
                onboardingUseCase,
                resolvePlaceCacheUseCase,
                postRepository,
                styleRepository,
                CLOCK
        );
    }

    @Test
    void createsScheduledPostWithPlaceAndStyles() {
        CreateCompanionPostResult result = service.create(command(
                CompanionPostMeetingTimeType.SCHEDULED,
                LocalDateTime.of(2026, 7, 3, 14, 0),
                true,
                List.of(CompanionPostKeyword.NEW_FOOD_CHALLENGE, CompanionPostKeyword.PHOTO_LOVER,
                        CompanionPostKeyword.NEW_FOOD_CHALLENGE)
        ));

        assertEquals(7L, onboardingUseCase.userId);
        assertEquals("google-place-id", resolvePlaceCacheUseCase.command.googlePlaceId());
        assertEquals("RESTAURANT", resolvePlaceCacheUseCase.command.category());
        assertEquals(101L, result.postId());
        assertEquals(CompanionPostStatus.RECRUITING, result.status());
        assertEquals(7L, result.hostUserId());
        assertEquals(20L, result.place().placeId());
        assertEquals(CompanionPostMeetingTimeType.SCHEDULED, result.meetingTimeType());
        assertEquals(LocalDateTime.of(2026, 7, 3, 14, 0), result.meetingAt());
        assertNull(result.exposureExpiresAt());
        assertEquals(4, result.maxParticipants());
        assertEquals(1, result.participantCount());
        assertEquals(true, result.departEvenIfNotFull());
        assertIterableEquals(
                List.of(CompanionPostKeyword.NEW_FOOD_CHALLENGE, CompanionPostKeyword.PHOTO_LOVER),
                result.styleKeywords()
        );
        assertEquals(NOW, result.createdAt());

        CompanionPost saved = postRepository.savedPost;
        assertEquals(CompanionPostMeetingTimeType.SCHEDULED, saved.meetingTimeType());
        assertEquals(LocalDateTime.of(2026, 7, 3, 14, 0), saved.meetingAt());
        assertEquals(CompanionPostStatus.RECRUITING, saved.status());
        assertIterableEquals(
                List.of(CompanionPostKeyword.NEW_FOOD_CHALLENGE, CompanionPostKeyword.PHOTO_LOVER),
                styleRepository.savedStyles.stream().map(CompanionPostStyle::keyword).toList()
        );
    }

    @Test
    void createsNowPostWithOneHourExposureExpiryAndDefaults() {
        CreateCompanionPostResult result = service.create(command(
                CompanionPostMeetingTimeType.NOW,
                null,
                null,
                null
        ));

        assertEquals(CompanionPostMeetingTimeType.NOW, result.meetingTimeType());
        assertNull(result.meetingAt());
        assertEquals(NOW.plusHours(1), result.exposureExpiresAt());
        assertEquals(true, result.departEvenIfNotFull());
        assertEquals(List.of(), result.styleKeywords());
        assertEquals(CompanionPostPlaceCategory.OTHER, result.place().category());
    }

    @Test
    void returnsResolvedPlaceSnapshot() {
        resolvePlaceCacheUseCase.result = new ResolvedPlaceCache(
                20L,
                "google-place-id",
                "저장된 장소명",
                "저장된 주소",
                new BigDecimal("37.11110000"),
                new BigDecimal("126.22220000"),
                "restaurant"
        );

        CreateCompanionPostResult result = service.create(command(
                CompanionPostMeetingTimeType.NOW,
                null,
                true,
                List.of()
        ));

        assertEquals("저장된 장소명", result.place().name());
        assertEquals("저장된 주소", result.place().address());
        assertEquals(new BigDecimal("37.11110000"), result.place().latitude());
        assertEquals(new BigDecimal("126.22220000"), result.place().longitude());
        assertEquals(CompanionPostPlaceCategory.RESTAURANT, result.place().category());
    }

    @Test
    void createsUndecidedPostWithoutMeetingAtAndExposureExpiry() {
        CreateCompanionPostResult result = service.create(command(
                CompanionPostMeetingTimeType.UNDECIDED,
                null,
                false,
                List.of(CompanionPostKeyword.POWER_P)
        ));

        assertNull(result.meetingAt());
        assertNull(result.exposureExpiresAt());
        assertEquals(false, result.departEvenIfNotFull());
    }

    @Test
    void rejectsInvalidCommand() {
        assertThrows(InvalidCompanionPostCreateRequestException.class, () -> service.create(command(
                CompanionPostMeetingTimeType.SCHEDULED,
                null,
                true,
                List.of()
        )));

        CreateCompanionPostCommand invalidLatitude = withPlaceLatitude(command(
                CompanionPostMeetingTimeType.NOW,
                null,
                true,
                List.of()
        ), new BigDecimal("91.00000000"));
        assertThrows(InvalidCompanionPostCreateRequestException.class, () -> service.create(invalidLatitude));

        CreateCompanionPostCommand longContent = withContent(command(
                CompanionPostMeetingTimeType.NOW,
                null,
                true,
                List.of()
        ), "a".repeat(101));
        assertThrows(InvalidCompanionPostCreateRequestException.class, () -> service.create(longContent));

        CreateCompanionPostCommand missingOpenChatUrl = withOpenChatUrl(command(
                CompanionPostMeetingTimeType.NOW,
                null,
                true,
                List.of()
        ), " ");
        assertThrows(InvalidCompanionPostCreateRequestException.class, () -> service.create(missingOpenChatUrl));

        assertThrows(InvalidCompanionPostCreateRequestException.class, () -> service.create(command(
                CompanionPostMeetingTimeType.SCHEDULED,
                NOW,
                true,
                List.of()
        )));

        assertThrows(InvalidCompanionPostCreateRequestException.class, () -> service.create(command(
                CompanionPostMeetingTimeType.SCHEDULED,
                NOW.minusMinutes(1),
                true,
                List.of()
        )));

        CreateCompanionPostCommand tooFewParticipants = withMaxParticipants(command(
                CompanionPostMeetingTimeType.NOW,
                null,
                true,
                List.of()
        ), 0);
        assertThrows(InvalidCompanionPostCreateRequestException.class, () -> service.create(tooFewParticipants));

        CreateCompanionPostCommand tooManyParticipants = withMaxParticipants(command(
                CompanionPostMeetingTimeType.NOW,
                null,
                true,
                List.of()
        ), 8);
        assertThrows(InvalidCompanionPostCreateRequestException.class, () -> service.create(tooManyParticipants));

        CreateCompanionPostCommand allCategory = withPlaceCategory(command(
                CompanionPostMeetingTimeType.SCHEDULED,
                NOW.plusHours(1),
                true,
                List.of()
        ), CompanionPostPlaceCategory.ALL);
        assertThrows(InvalidCompanionPostCreateRequestException.class, () -> service.create(allCategory));
    }

    @Test
    void rejectsInvalidOpenChatUrl() {
        CreateCompanionPostCommand command = withOpenChatUrl(command(
                CompanionPostMeetingTimeType.NOW,
                null,
                true,
                List.of()
        ), "https://example.com/o/nearby");

        assertThrows(InvalidOpenChatUrlException.class, () -> service.create(command));
    }

    @Test
    void rejectsUserWithoutCompletedOnboarding() {
        onboardingUseCase.exception = new OnboardingRequiredException();

        assertThrows(OnboardingRequiredException.class, () -> service.create(command(
                CompanionPostMeetingTimeType.NOW,
                null,
                true,
                List.of()
        )));
    }

    private CreateCompanionPostCommand command(
            final CompanionPostMeetingTimeType meetingTimeType,
            final LocalDateTime meetingAt,
            final Boolean departEvenIfNotFull,
            final List<CompanionPostKeyword> styleKeywords
    ) {
        return new CreateCompanionPostCommand(
                7L,
                new CreateCompanionPostCommand.Place(
                        "google-place-id",
                        "니어바이 스시",
                        "서울특별시 중구 세종대로 110",
                        new BigDecimal("37.56710000"),
                        new BigDecimal("126.97920000"),
                        meetingTimeType == CompanionPostMeetingTimeType.NOW ? null : CompanionPostPlaceCategory.RESTAURANT
                ),
                meetingTimeType,
                meetingAt,
                4,
                departEvenIfNotFull,
                styleKeywords,
                "같이 스시 먹으러 갈 사람 구해요.",
                "https://open.kakao.com/o/nearby123"
        );
    }

    private CreateCompanionPostCommand withPlaceLatitude(
            final CreateCompanionPostCommand command,
            final BigDecimal latitude
    ) {
        return new CreateCompanionPostCommand(
                command.hostUserId(),
                new CreateCompanionPostCommand.Place(
                        command.place().googlePlaceId(),
                        command.place().name(),
                        command.place().address(),
                        latitude,
                        command.place().longitude(),
                        command.place().category()
                ),
                command.meetingTimeType(),
                command.meetingAt(),
                command.maxParticipants(),
                command.departEvenIfNotFull(),
                command.styleKeywords(),
                command.content(),
                command.openChatUrl()
        );
    }

    private CreateCompanionPostCommand withContent(
            final CreateCompanionPostCommand command,
            final String content
    ) {
        return new CreateCompanionPostCommand(
                command.hostUserId(),
                command.place(),
                command.meetingTimeType(),
                command.meetingAt(),
                command.maxParticipants(),
                command.departEvenIfNotFull(),
                command.styleKeywords(),
                content,
                command.openChatUrl()
        );
    }

    private CreateCompanionPostCommand withOpenChatUrl(
            final CreateCompanionPostCommand command,
            final String openChatUrl
    ) {
        return new CreateCompanionPostCommand(
                command.hostUserId(),
                command.place(),
                command.meetingTimeType(),
                command.meetingAt(),
                command.maxParticipants(),
                command.departEvenIfNotFull(),
                command.styleKeywords(),
                command.content(),
                openChatUrl
        );
    }

    private CreateCompanionPostCommand withMaxParticipants(
            final CreateCompanionPostCommand command,
            final int maxParticipants
    ) {
        return new CreateCompanionPostCommand(
                command.hostUserId(),
                command.place(),
                command.meetingTimeType(),
                command.meetingAt(),
                maxParticipants,
                command.departEvenIfNotFull(),
                command.styleKeywords(),
                command.content(),
                command.openChatUrl()
        );
    }

    private CreateCompanionPostCommand withPlaceCategory(
            final CreateCompanionPostCommand command,
            final CompanionPostPlaceCategory category
    ) {
        return new CreateCompanionPostCommand(
                command.hostUserId(),
                new CreateCompanionPostCommand.Place(
                        command.place().googlePlaceId(),
                        command.place().name(),
                        command.place().address(),
                        command.place().latitude(),
                        command.place().longitude(),
                        category
                ),
                command.meetingTimeType(),
                command.meetingAt(),
                command.maxParticipants(),
                command.departEvenIfNotFull(),
                command.styleKeywords(),
                command.content(),
                command.openChatUrl()
        );
    }

    private static final class FakeRequireCompletedOnboardingUseCase implements RequireCompletedOnboardingUseCase {

        private Long userId;
        private RuntimeException exception;

        @Override
        public void requireCompleted(final Long userId) {
            this.userId = userId;
            if (exception != null) {
                throw exception;
            }
        }
    }

    private static final class FakeResolvePlaceCacheUseCase implements ResolvePlaceCacheUseCase {

        private ResolvePlaceCacheCommand command;
        private ResolvedPlaceCache result;

        @Override
        public ResolvedPlaceCache resolve(final ResolvePlaceCacheCommand command) {
            this.command = command;
            if (result != null) {
                return result;
            }
            return new ResolvedPlaceCache(
                    20L,
                    command.googlePlaceId(),
                    command.name(),
                    command.address(),
                    command.latitude(),
                    command.longitude(),
                    command.category() == null ? "OTHER" : command.category()
            );
        }
    }

    private static final class FakeCompanionPostRepository implements CompanionPostRepository {

        private CompanionPost savedPost;

        @Override
        public CompanionPost save(final CompanionPost model) {
            savedPost = new CompanionPost(
                    101L,
                    model.hostUserId(),
                    model.placeId(),
                    model.meetingTimeType(),
                    model.meetingAt(),
                    model.exposureExpiresAt(),
                    model.maxParticipants(),
                    model.departEvenIfNotFull(),
                    model.content(),
                    model.openChatUrl(),
                    model.status(),
                    model.createdAt()
            );
            return savedPost;
        }

        @Override
        public Optional<CompanionPost> findById(final Long id) {
            return Optional.empty();
        }
    }

    private static final class FakeCompanionPostStyleRepository implements CompanionPostStyleRepository {

        private final List<CompanionPostStyle> savedStyles = new ArrayList<>();

        @Override
        public CompanionPostStyle save(final CompanionPostStyle model) {
            savedStyles.add(model);
            return model;
        }

        @Override
        public Optional<CompanionPostStyle> findById(final CompanionPostStyle.Key key) {
            return Optional.empty();
        }
    }
}
