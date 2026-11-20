// 동행 모집글 목록 조회 유스케이스를 구현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionPostSearchRequestException;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
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
    private static final Duration TIME_ZONE_LOOKUP_BUDGET = Duration.ofSeconds(1);

    private final CompanionPostQueryPort queryPort;
    private final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase;
    private final Clock clock;
    private final ResolvePlaceImageUseCase resolvePlaceImageUseCase;
    private final CompanionPlaceTimeResolver placeTimeResolver;
    private final Duration timeZoneLookupBudget;
    private final LongSupplier nanoTime;

    public ReadCompanionPostsService(
            final CompanionPostQueryPort queryPort,
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase,
            final Clock clock,
            final ResolvePlaceImageUseCase resolvePlaceImageUseCase
    ) {
        this(queryPort, requireCompletedOnboardingUseCase, clock, resolvePlaceImageUseCase,
                new CompanionPlaceTimeResolver());
    }

    public ReadCompanionPostsService(
            final CompanionPostQueryPort queryPort,
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase,
            final Clock clock,
            final ResolvePlaceImageUseCase resolvePlaceImageUseCase,
            final CompanionPlaceTimeResolver placeTimeResolver
    ) {
        this(queryPort, requireCompletedOnboardingUseCase, clock, resolvePlaceImageUseCase,
                placeTimeResolver, TIME_ZONE_LOOKUP_BUDGET, System::nanoTime);
    }

    ReadCompanionPostsService(
            final CompanionPostQueryPort queryPort,
            final RequireCompletedOnboardingUseCase requireCompletedOnboardingUseCase,
            final Clock clock,
            final ResolvePlaceImageUseCase resolvePlaceImageUseCase,
            final CompanionPlaceTimeResolver placeTimeResolver,
            final Duration timeZoneLookupBudget,
            final LongSupplier nanoTime
    ) {
        this.queryPort = queryPort;
        this.requireCompletedOnboardingUseCase = requireCompletedOnboardingUseCase;
        this.clock = clock;
        this.resolvePlaceImageUseCase = resolvePlaceImageUseCase;
        this.placeTimeResolver = placeTimeResolver;
        this.timeZoneLookupBudget = timeZoneLookupBudget;
        this.nanoTime = nanoTime;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanionPostsResult read(final ReadCompanionPostsCommand command) {
        validate(command);
        if (command.userId() != null) {
            requireCompletedOnboardingUseCase.requireCompleted(command.userId());
        }

        List<CompanionPostSummary> summaries = queryPort.find(command);
        var instant = clock.instant();
        long lookupDeadline = nanoTime.getAsLong() + timeZoneLookupBudget.toNanos();
        Map<String, ZoneId> zones = new HashMap<>();
        List<CompanionPostsResult.Post> posts = summaries.stream()
                .map(summary -> toPost(summary, instant, lookupDeadline, zones))
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

    private CompanionPostsResult.Post toPost(
            final CompanionPostSummary summary,
            final Instant instant,
            final long lookupDeadline,
            final Map<String, ZoneId> zones
    ) {
        String contentPreview = contentPreview(summary.content());
        var zone = zones.computeIfAbsent(coordinateKey(summary), ignored -> nanoTime.getAsLong() < lookupDeadline
                ? placeTimeResolver.resolveZone(
                        summary.placeAddress(), summary.latitude(), summary.longitude(), instant)
                : placeTimeResolver.resolveZone(summary.placeAddress(), null, null, instant));
        var meetingAt = CompanionMeetingTimeConverter.utcToLocal(summary.meetingAt(), zone);
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
                summary.meetingTimeType(),
                meetingAt,
                meetingAtText(meetingAt),
                summary.participantCount(),
                summary.maxParticipants(),
                summary.participants().stream()
                        .map(participant -> new CompanionPostsResult.Participant(
                                participant.userId(),
                                participant.profileImageUrl()
                        ))
                        .toList(),
                summary.participantCount() + "/" + summary.maxParticipants() + " 모집 중",
                CompanionMeetingTimeConverter.utcToLocal(summary.createdAt(), zone),
                createdAgoText(summary.createdAt(), zone),
                mapMarkerText(meetingAt, summary.placeName())
        );
    }

    private String coordinateKey(final CompanionPostSummary summary) {
        if (summary.latitude() == null || summary.longitude() == null) {
            return "address:" + summary.placeAddress();
        }
        return summary.latitude().stripTrailingZeros().toPlainString()
                + "," + summary.longitude().stripTrailingZeros().toPlainString();
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

    private String createdAgoText(final LocalDateTime createdAt, final ZoneId zone) {
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
        return DATE_TEXT_FORMATTER.format(CompanionMeetingTimeConverter.utcToLocal(createdAt, zone));
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
                || (command.userId() != null && command.userId() <= 0)
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
