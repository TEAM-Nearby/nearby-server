// 동행 프로필 상세 조회에 필요한 프로필 정보를 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model.profile;

import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompanionProfileDetail(
        Long profileId,
        Long userId,
        String nickname,
        UserGender gender,
        Integer birthYear,
        String profileImageUrl,
        String intro,
        BigDecimal mannerScore,
        int reviewCount,
        CompanionProfileStatus status,
        LocalDateTime phoneVerifiedAt,
        List<TravelStyleKeyword> keywords,
        List<ReviewKeyword> mannerKeywords
) {

    public CompanionProfileDetail {
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
        mannerKeywords = mannerKeywords == null ? List.of() : List.copyOf(mannerKeywords);
    }
}
