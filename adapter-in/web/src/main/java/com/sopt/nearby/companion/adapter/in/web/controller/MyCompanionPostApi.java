// 내가 작성한 동행 모집글 API 문서를 정의한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.response.MyCompanionPostsResponse;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "MyCompanionPost", description = "내 동행 모집글 API")
public interface MyCompanionPostApi {

	@Operation(
			summary = "내가 작성한 동행 모집글 목록 조회",
			description = """
					JWT 액세스 토큰으로 인증된 사용자가 직접 작성한 동행 모집글 목록을 조회합니다.
					scheduledAt은 ISO-8601 형식으로 반환하며 화면 포맷팅과 null 표시는 클라이언트에서 처리합니다.
					hostProfileImageUrl은 작성자의 프로필 이미지이며, members는 호스트를 제외한 참여 확정 멤버 목록입니다.
					프로필 이미지 URL이 null인 경우 기본 이미지 표시는 클라이언트에서 처리합니다.
					지도 표시는 googlePlaceId와 위경도를 사용해 클라이언트에서 처리합니다.
					""",
			security = @SecurityRequirement(name = "bearerAuth")
	)
	CommonResponse<MyCompanionPostsResponse> getMyPosts(
			@Parameter(hidden = true)
			Principal principal
	);
}
