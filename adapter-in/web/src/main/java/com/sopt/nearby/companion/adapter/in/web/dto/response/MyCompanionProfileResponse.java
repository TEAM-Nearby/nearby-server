// 프로필 수정 화면에 필요한 본인 프로필 정보를 응답한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.MyCompanionProfileResult;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MyCompanionProfileResponse(
        Long profileId,
        String nickname,
        @Schema(description = "조회 전용 성별. 수정할 수 없습니다.") UserGender gender,
        @Schema(description = "조회 전용 출생연도. 미등록이면 null이며 수정할 수 없습니다.") Integer birthYear,
        String intro,
        String profileImageUrl,
        List<TravelStyleKeyword> travelStyleKeywords
) {
    public static MyCompanionProfileResponse from(final MyCompanionProfileResult result) {
        return new MyCompanionProfileResponse(result.profileId(), result.nickname(), result.gender(),
                result.birthYear(), result.intro(), result.profileImageUrl(), result.travelStyleKeywords());
    }
}
