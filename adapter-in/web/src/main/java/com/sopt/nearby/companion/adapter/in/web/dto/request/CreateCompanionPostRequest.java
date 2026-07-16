// 동행 모집 글 작성 요청 본문을 유스케이스 명령으로 변환한다.
package com.sopt.nearby.companion.adapter.in.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sopt.nearby.companion.application.CreateCompanionPostCommand;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionPostCreateRequestException;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostKeyword;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateCompanionPostRequest(
        PlaceRequest place,
        String meetingTimeType,
        LocalDateTime meetingAt,
        @JsonAlias("maxParticipants")
        @Schema(description = "작성자를 제외하고 모집할 인원", example = "1", minimum = "1", maximum = "6")
        int recruitmentCapacity,
        Boolean departEvenIfNotFull,
        List<String> styleKeywords,
        String content,
        String openChatUrl
) {

    public CreateCompanionPostCommand toCommand(final Long hostUserId) {
        try {
            return new CreateCompanionPostCommand(
                    hostUserId,
                    place == null ? null : place.toCommandPlace(),
                    parseMeetingTimeType(meetingTimeType),
                    meetingAt,
                    recruitmentCapacity,
                    departEvenIfNotFull,
                    parseStyleKeywords(styleKeywords),
                    content,
                    openChatUrl
            );
        } catch (IllegalArgumentException exception) {
            throw new InvalidCompanionPostCreateRequestException();
        }
    }

    private CompanionPostMeetingTimeType parseMeetingTimeType(final String value) {
        return CompanionPostMeetingTimeType.valueOf(required(value).toUpperCase(Locale.ROOT));
    }

    private List<CompanionPostKeyword> parseStyleKeywords(final List<String> values) {
        if (values == null) {
            return null;
        }
        return values.stream()
                .map(value -> CompanionPostKeyword.valueOf(required(value).toUpperCase(Locale.ROOT)))
                .toList();
    }

    private String required(final String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidCompanionPostCreateRequestException();
        }
        return value;
    }

    public record PlaceRequest(
            String googlePlaceId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String category
    ) {

        CreateCompanionPostCommand.Place toCommandPlace() {
            return new CreateCompanionPostCommand.Place(
                    googlePlaceId,
                    name,
                    address,
                    latitude,
                    longitude,
                    category == null || category.isBlank()
                            ? null
                            : CompanionPostPlaceCategory.valueOf(category.toUpperCase(Locale.ROOT))
            );
        }
    }
}
