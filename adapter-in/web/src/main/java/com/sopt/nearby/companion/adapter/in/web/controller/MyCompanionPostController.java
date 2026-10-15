// 내가 작성한 동행 모집글 HTTP 요청을 처리한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.response.MyCompanionPostsResponse;
import com.sopt.nearby.companion.application.ReadMyCompanionPostsResult;
import com.sopt.nearby.companion.port.in.ReadMyCompanionPostsUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/recruitment-posts")
public class MyCompanionPostController implements MyCompanionPostApi {

	private final ReadMyCompanionPostsUseCase readMyCompanionPostsUseCase;

	public MyCompanionPostController(final ReadMyCompanionPostsUseCase readMyCompanionPostsUseCase) {
		this.readMyCompanionPostsUseCase = readMyCompanionPostsUseCase;
	}

	@Override
	@GetMapping
	public CommonResponse<MyCompanionPostsResponse> getMyPosts(final Principal principal) {
		Long userId = Long.valueOf(principal.getName());
		ReadMyCompanionPostsResult result = readMyCompanionPostsUseCase.getPosts(userId);

		return CommonResponse.success(
				CompanionSuccessCode.READ_MY_COMPANION_POSTS,
				MyCompanionPostsResponse.from(result)
		);
	}
}
