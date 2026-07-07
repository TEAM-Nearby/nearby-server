// 동행 모집글 목록 조회 유스케이스를 구현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionPostSearchRequestException;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostSummary;
import com.sopt.nearby.companion.port.in.ReadCompanionPostsUseCase;
import com.sopt.nearby.companion.port.out.CompanionPostQueryPort;
import com.sopt.nearby.place.port.in.ResolvePlaceImageCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.user.port.in.RequireCompletedOnboardingUseCase;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class ReadCompanionPostsService implements ReadCompanionPostsUseCase {

    public static final int MAX_RADIUS_METERS = 5000;
    private static final int CONTENT_PREVIEW_LENGTH = 49;
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final DateTimeFormatter DATE_TIME_TEXT_FORMATTER = DateTimeFormatter.ofPattern("M월 d일 HH:mm");
    private static final DateTimeFormatter DATE_TEXT_FORMATTER = DateTimeFormatter.ofPattern("M월 d일");

    private final CompanionPostQueryPort queryPort;
    private final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase;
    private final Clock clock;
    private final ResolvePlaceImageUseCase resolvePlaceImageUseCase;

    public ReadCompanionPostsService(
            final CompanionPostQueryPort queryPort,
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase,
            final Clock clock,
            final ResolvePlaceImageUseCase resolvePlaceImageUseCase
    ) {
        this.queryPort = queryPort;
        this.requireCompletedOnboardingUseCase = requireCompletedOnboardingUseCase;
        this.clock = clock;
        this.resolvePlaceImageUseCase = resolvePlaceImageUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanionPostsResult read(final ReadCompanionPostsCommand command) {
        validate(command);
        requireCompletedOnboardingUseCase.requireCompleted(command.userId());

        List<CompanionPostSummary> summaries = queryPort.find(command);
        List<CompanionPostsResult.Post> posts = summaries.stream()
                .map(this::toPost)
                .toList();

        return new CompanionPostsResult(
                new CompanionPostsResult.CurrentLocation(command.latitude(), command.longitude()),
                command.radiusMeters(),
                MAX_RADIUS_METERS,
                command.placeCategory(),
                command.sort(),
                posts.size(),
                "내 주변 " + posts.size() + "개의 동행이 있어요",
                posts
        );
    }

    private CompanionPostsResult.Post toPost(final CompanionPostSummary summary) {
        String contentPreview = contentPreview(summary.content());
        ResolvedPlaceImage image = resolvePlaceImageUseCase.resolve(new ResolvePlaceImageCommand(
                summary.googlePlaceId(),
                summary.photoReference()
        ));

        CompanionPostsResult.Place place = new CompanionPostsResult.Place(
                summary.placeId(),
                summary.googlePlaceId(),
                summary.placeName(),
                summary.placeCategory() == null ? CompanionPostPlaceCategory.OTHER : summary.placeCategory(),
                summary.latitude(),
                summary.longitude(),
                summary.distanceMeters(),
                image.imageUrl(),
                image.imageSource(),
                image.imageAttributions()
                        .stream()
                        .map(attribution -> new CompanionPostsResult.ImageAttribution(
                                attribution.displayName(),
                                attribution.uri(),
                                attribution.photoUri()
                        ))
                        .toList()
        );

        return new CompanionPostsResult.Post(
                summary.postId(),
                summary.status(),
                new CompanionPostsResult.Host(summary.hostNickname(), summary.hostGender()),
                place,
                contentPreview,
                summary.content() != null && summary.content().length() > CONTENT_PREVIEW_LENGTH,
                summary.meetingAt(),
                meetingAtText(summary.meetingAt()),
                summary.participantCount(),
                summary.maxParticipants(),
                summary.participantCount() + "/" + summary.maxParticipants() + " 모집 중",
                summary.createdAt(),
                createdAgoText(summary.createdAt()),
                mapMarkerText(summary.meetingAt(), summary.placeName())
        );
    }

    private String meetingAtText(final LocalDateTime meetingAt) {
        return meetingAt == null ? null : DATE_TIME_TEXT_FORMATTER.format(meetingAt);
    }

    private String contentPreview(final String content) {
        if (content == null || content.length() <= CONTENT_PREVIEW_LENGTH) {
            return content;
        }
        return content.substring(0, CONTENT_PREVIEW_LENGTH);
    }

    private String createdAgoText(final LocalDateTime createdAt) {
        long minutes = Duration.between(createdAt, LocalDateTime.now(clock)).toMinutes();
        if (minutes < 1) {
            return "방금 전";
        }
        if (minutes < 60) {
            return minutes + "분 전";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "시간 전";
        }
        return DATE_TEXT_FORMATTER.format(createdAt);
    }

    private String mapMarkerText(final LocalDateTime meetingAt, final String placeName) {
        if (meetingAt == null) {
            return placeName + " 동행";
        }
        String timeText = meetingAt.getMinute() == 0
                ? meetingAt.getHour() + "시"
                : meetingAt.getHour() + "시 " + meetingAt.getMinute() + "분";
        return DATE_TEXT_FORMATTER.format(meetingAt) + " " + timeText + " " + placeName + " 동행";
    }

    private void validate(final ReadCompanionPostsCommand command) {
        if (command == null
                || command.userId() == null
                || command.latitude() == null
                || command.longitude() == null
                || command.radiusMeters() <= 0
                || command.radiusMeters() > MAX_RADIUS_METERS
                || command.placeCategory() == null
                || (command.placeId() != null && command.placeId() <= 0)
                || command.sort() == null
                || command.latitude().compareTo(MIN_LATITUDE) < 0
                || command.latitude().compareTo(MAX_LATITUDE) > 0
                || command.longitude().compareTo(MIN_LONGITUDE) < 0
                || command.longitude().compareTo(MAX_LONGITUDE) > 0) {
            throw new InvalidCompanionPostSearchRequestException();
        }
    }
}
