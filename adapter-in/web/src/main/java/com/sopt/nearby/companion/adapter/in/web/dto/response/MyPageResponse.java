// 마이페이지 조회 API 응답을 표현한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.ReadMyPageResult;
import java.math.BigDecimal;
import java.util.List;

public record MyPageResponse(
        String profileImageUrl,
        String nickname,
        boolean isPhoneVerified,
        String ageGroup,
        String gender,
        BigDecimal mannerScore,
        List<String> mannerKeywords,
        List<String> travelStyleKeywords,
        int mealTogetherCount,
        int visitedCityCount,
        int receivedReviewCount
) {

    public static MyPageResponse from(final ReadMyPageResult result) {
        return new MyPageResponse(
                result.profileImageUrl(),
                result.nickname(),
                result.phoneVerified(),
                result.ageGroup() == null ? null : result.ageGroup().name(),
                result.gender().name(),
                result.mannerScore(),
                result.mannerKeywords().stream()
                        .map(Enum::name)
                        .toList(),
                result.travelStyleKeywords().stream()
                        .map(Enum::name)
                        .toList(),
                result.mealTogetherCount(),
                result.visitedCityCount(),
                result.receivedReviewCount()
        );
    }
}
