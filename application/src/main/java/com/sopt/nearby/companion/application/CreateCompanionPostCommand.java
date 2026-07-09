// 동행 모집 글 작성 요청 값을 애플리케이션 계층으로 전달한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostKeyword;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreateCompanionPostCommand(
        Long hostUserId,
        Place place,
        CompanionPostMeetingTimeType meetingTimeType,
        LocalDateTime meetingAt,
        int maxParticipants,
        Boolean departEvenIfNotFull,
        List<CompanionPostKeyword> styleKeywords,
        String content,
        String openChatUrl
) {

    public record Place(
            String googlePlaceId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            CompanionPostPlaceCategory category
    ) {
    }
}
