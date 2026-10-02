// 본인 동행 프로필의 수정 요청을 유스케이스 입력으로 변환한다.
package com.sopt.nearby.companion.adapter.in.web.dto.request;

import com.sopt.nearby.companion.application.UpdateMyCompanionProfileCommand;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateMyCompanionProfileRequest(
        @Schema(description = "닉네임. 앞뒤 공백 제거 후 최대 15자이며 본인의 기존 닉네임은 재사용할 수 있습니다.", example = "여행친구")
        @NotBlank
        String nickname,

        @Schema(description = "한줄소개. 앞뒤 공백 제거 후 최대 50자이며 생략, null, 빈 문자열 또는 공백이면 삭제합니다.", example = "함께 걸어요")
        String intro,

        @Schema(description = "업로드 완료된 HTTP(S) 이미지 URL. 앞뒤 공백 제거 후 최대 255자이며 생략, null, 빈 문자열 또는 공백이면 삭제합니다.")
        String profileImageUrl,

        @Schema(description = "전체 여행 스타일 목록. 한 개 이상이며 중복을 허용하지 않습니다.",
                example = "[\"EXTROVERTED\", \"CAFE_TOUR\"]")
        @NotEmpty
        List<@NotNull TravelStyleKeyword> travelStyleKeywords
) {
    public UpdateMyCompanionProfileCommand toCommand(final Long userId) {
        return new UpdateMyCompanionProfileCommand(userId, nickname, intro, profileImageUrl, travelStyleKeywords);
    }
}
