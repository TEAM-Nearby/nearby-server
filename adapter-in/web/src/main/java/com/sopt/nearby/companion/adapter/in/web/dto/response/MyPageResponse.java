// 마이페이지 조회 API 응답을 표현한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.ReadMyPageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record MyPageResponse(
        @Schema(description = "본인의 동행 프로필 ID. 프로필 등록을 건너뛴 경우 null입니다.",
                example = "5", types = {"integer", "null"})
        Long profileId,
        String profileImageUrl,
        String nickname,
        @Schema(description = "저장된 한 줄 소개. 소개가 없거나 프로필 등록을 건너뛴 경우 null입니다.",
                example = "맛집을 함께 찾아다니는 여행을 좋아해요.", types = {"string", "null"})
        String intro,
        boolean isPhoneVerified,
        String ageGroup,
        String gender,
        BigDecimal mannerScore,
        List<String> mannerKeywords,
        List<String> travelStyleKeywords,
        int mealTogetherCount,
        int visitedCityCount,
        int receivedReviewCount,
        boolean hasCompanionProfile
) {

    public static MyPageResponse from(final ReadMyPageResult result) {
        return new MyPageResponse(
                result.profileId(),
                result.profileImageUrl(),
                result.nickname(),
                result.intro(),
                result.phoneVerified(),
                result.ageGroup() == null ? null : result.ageGroup().name(),
                result.gender() == null ? null : result.gender().name(),
                result.mannerScore(),
                result.mannerKeywords().stream()
                        .map(Enum::name)
                        .toList(),
                result.travelStyleKeywords().stream()
                        .map(Enum::name)
                        .toList(),
                result.mealTogetherCount(),
                result.visitedCityCount(),
                result.receivedReviewCount(),
                result.hasCompanionProfile()
        );
    }
}
