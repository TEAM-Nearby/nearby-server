// 마이페이지 조회 결과를 표현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.math.BigDecimal;
import java.util.List;

public record ReadMyPageResult(
        String profileImageUrl,
        String nickname,
        boolean phoneVerified,
        AgeGroup ageGroup,
        UserGender gender,
        BigDecimal mannerScore,
        List<ReviewKeyword> mannerKeywords,
        List<TravelStyleKeyword> travelStyleKeywords,
        int mealTogetherCount,
        int visitedCityCount,
        int receivedReviewCount
) {

    public ReadMyPageResult {
        mannerKeywords = mannerKeywords == null ? List.of() : List.copyOf(mannerKeywords);
        travelStyleKeywords = travelStyleKeywords == null ? List.of() : List.copyOf(travelStyleKeywords);
    }

    public enum AgeGroup {
        TEENS,
        TWENTIES,
        THIRTIES,
        FORTIES,
        FIFTIES,
        SIXTIES_OR_ABOVE
    }
}
