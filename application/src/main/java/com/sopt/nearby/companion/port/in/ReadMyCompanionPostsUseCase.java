// 내가 작성한 동행 모집글 목록 조회 유스케이스 진입점을 정의한다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.ReadMyCompanionPostsResult;

public interface ReadMyCompanionPostsUseCase {

	ReadMyCompanionPostsResult getPosts(Long userId);
}
