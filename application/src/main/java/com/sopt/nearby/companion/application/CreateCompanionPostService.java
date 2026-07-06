// 동행 모집 글 작성 유스케이스를 구현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionPostCreateRequestException;
import com.sopt.nearby.companion.domain.exception.InvalidOpenChatUrlException;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStyle;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.in.CreateCompanionPostUseCase;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import com.sopt.nearby.companion.port.out.CompanionPostStyleRepository;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceCache;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class CreateCompanionPostService implements CreateCompanionPostUseCase {

    private static final String OPEN_CHAT_URL_PREFIX = "https://open.kakao.com/";
    private static final int MIN_PARTICIPANTS = 1;
    private static final int MAX_PARTICIPANTS = 7;
    private static final int MAX_CONTENT_LENGTH_EXCLUDING_WHITESPACE = 100;
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");

    private final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase;
    private final ResolvePlaceCacheUseCase resolvePlaceCacheUseCase;
    private final CompanionPostRepository postRepository;
    private final CompanionPostStyleRepository styleRepository;
    private final Clock clock;

    public CreateCompanionPostService(
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase,
            final ResolvePlaceCacheUseCase resolvePlaceCacheUseCase,
            final CompanionPostRepository postRepository,
            final CompanionPostStyleRepository styleRepository,
            final Clock clock
    ) {
        this.requireCompletedOnboardingUseCase = requireCompletedOnboardingUseCase;
        this.resolvePlaceCacheUseCase = resolvePlaceCacheUseCase;
        this.postRepository = postRepository;
        this.styleRepository = styleRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreateCompanionPostResult create(final CreateCompanionPostCommand command) {
        validate(command);
        validateOpenChatUrl(command.openChatUrl());
        requireCompletedOnboardingUseCase.requireCompleted(command.hostUserId());

        LocalDateTime createdAt = LocalDateTime.now(clock);
        CompanionPostPlaceCategory category = effectiveCategory(command.place().category());
        List<TravelStyleKeyword> styleKeywords = distinct(command.styleKeywords());
        LocalDateTime meetingAt = command.meetingTimeType() == CompanionPostMeetingTimeType.SCHEDULED
                ? command.meetingAt()
                : null;
        LocalDateTime exposureExpiresAt = command.meetingTimeType() == CompanionPostMeetingTimeType.NOW
                ? createdAt.plusHours(1)
                : null;

        ResolvedPlaceCache place = resolvePlaceCacheUseCase.resolve(new ResolvePlaceCacheCommand(
                command.place().googlePlaceId(),
                command.place().name(),
                command.place().address(),
                command.place().latitude(),
                command.place().longitude(),
                command.place().category() == null ? null : command.place().category().name()
        ));

        CompanionPost post = postRepository.save(new CompanionPost(
                null,
                command.hostUserId(),
                place.placeId(),
                command.meetingTimeType(),
                meetingAt,
                exposureExpiresAt,
                command.maxParticipants(),
                command.departEvenIfNotFull() == null || command.departEvenIfNotFull(),
                command.content(),
                command.openChatUrl(),
                CompanionPostStatus.RECRUITING,
                createdAt
        ));

        styleKeywords.forEach(keyword -> styleRepository.save(new CompanionPostStyle(post.id(), keyword)));

        return new CreateCompanionPostResult(
                post.id(),
                post.status(),
                post.hostUserId(),
                new CreateCompanionPostResult.Place(
                        place.placeId(),
                        command.place().googlePlaceId(),
                        command.place().name(),
                        command.place().address(),
                        command.place().latitude(),
                        command.place().longitude(),
                        category
                ),
                post.meetingTimeType(),
                post.meetingAt(),
                post.exposureExpiresAt(),
                post.maxParticipants(),
                1,
                post.departEvenIfNotFull(),
                styleKeywords,
                post.content(),
                post.openChatUrl(),
                post.createdAt()
        );
    }

    private void validate(final CreateCompanionPostCommand command) {
        if (command == null
                || command.hostUserId() == null
                || command.place() == null
                || isBlank(command.place().googlePlaceId())
                || isBlank(command.place().name())
                || command.place().latitude() == null
                || command.place().longitude() == null
                || command.meetingTimeType() == null
                || command.maxParticipants() < MIN_PARTICIPANTS
                || command.maxParticipants() > MAX_PARTICIPANTS
                || isBlank(command.content())
                || isBlank(command.openChatUrl())
                || command.place().category() == CompanionPostPlaceCategory.ALL
                || isInvalidCoordinate(command.place().latitude(), MIN_LATITUDE, MAX_LATITUDE)
                || isInvalidCoordinate(command.place().longitude(), MIN_LONGITUDE, MAX_LONGITUDE)
                || contentLength(command.content()) > MAX_CONTENT_LENGTH_EXCLUDING_WHITESPACE
                || isInvalidMeetingTime(command)) {
            throw new InvalidCompanionPostCreateRequestException();
        }
    }

    private boolean isInvalidMeetingTime(final CreateCompanionPostCommand command) {
        return switch (command.meetingTimeType()) {
            case SCHEDULED -> command.meetingAt() == null;
            case NOW, UNDECIDED -> command.meetingAt() != null;
        };
    }

    private void validateOpenChatUrl(final String openChatUrl) {
        if (isBlank(openChatUrl) || !openChatUrl.startsWith(OPEN_CHAT_URL_PREFIX)) {
            throw new InvalidOpenChatUrlException();
        }
    }

    private CompanionPostPlaceCategory effectiveCategory(final CompanionPostPlaceCategory category) {
        return category == null ? CompanionPostPlaceCategory.OTHER : category;
    }

    private boolean isInvalidCoordinate(final BigDecimal value, final BigDecimal min, final BigDecimal max) {
        return value.compareTo(min) < 0 || value.compareTo(max) > 0;
    }

    private int contentLength(final String content) {
        return content.replaceAll("\\s", "").length();
    }

    private List<TravelStyleKeyword> distinct(final List<TravelStyleKeyword> styleKeywords) {
        if (styleKeywords == null) {
            return List.of();
        }
        return List.copyOf(new LinkedHashSet<>(styleKeywords));
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
