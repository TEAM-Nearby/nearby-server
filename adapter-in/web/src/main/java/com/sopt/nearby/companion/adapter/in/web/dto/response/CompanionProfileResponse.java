// 동행 프로필 상세 조회 API 응답을 표현한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.profile.CompanionProfileDetail;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompanionProfileResponse(
        Long profileId,
        Long userId,
        String nickname,
        String gender,
        Integer birthYear,
        String profileImageUrl,
        String intro,
        BigDecimal mannerScore,
        int reviewCount,
        String status,
        LocalDateTime phoneVerifiedAt,
        List<String> keywords
) {

    public static CompanionProfileResponse from(final CompanionProfileDetail result) {
        return new CompanionProfileResponse(
                result.profileId(),
                result.userId(),
                result.nickname(),
                result.gender().name(),
                result.birthYear(),
                result.profileImageUrl(),
                result.intro(),
                result.mannerScore(),
                result.reviewCount(),
                result.status().name(),
                result.phoneVerifiedAt(),
                result.keywords().stream()
                        .map(TravelStyleKeyword::name)
                        .toList()
        );
    }
}
