// 동행 모집글에 연결된 여행 성향 키워드를 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model.post;

public record CompanionPostStyle(
		Long postId,
		CompanionPostKeyword keyword
) {

	public Key key() {
		return new Key(postId, keyword);
	}

	public record Key(Long postId, CompanionPostKeyword keyword) {
	}
}
