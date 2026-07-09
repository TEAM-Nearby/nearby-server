// 마이페이지 조회에 필요한 동행 프로필과 활동 요약을 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model.profile;

import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MyPageProfile(
        Long profileId,
        Long userId,
        String nickname,
        UserGender gender,
        Integer birthYear,
        String profileImageUrl,
        BigDecimal mannerScore,
        int reviewCount,
        LocalDateTime phoneVerifiedAt,
        List<TravelStyleKeyword> travelStyleKeywords,
        List<ReviewKeyword> mannerKeywords,
        List<CompletedMeetingPlace> completedMeetingPlaces
) {

    public MyPageProfile {
        travelStyleKeywords = travelStyleKeywords == null ? List.of() : List.copyOf(travelStyleKeywords);
        mannerKeywords = mannerKeywords == null ? List.of() : List.copyOf(mannerKeywords);
        completedMeetingPlaces = completedMeetingPlaces == null ? List.of() : List.copyOf(completedMeetingPlaces);
    }

    public record CompletedMeetingPlace(
            String name,
            String address
    ) {
    }
}
