// 동행 프로필에 연결된 여행 성향 키워드를 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model;

public record CompanionProfileStyle(
		Long profileId,
		TravelStyleKeyword keyword
) {

	public Key key() {
		return new Key(profileId, keyword);
	}

	public record Key(Long profileId, TravelStyleKeyword keyword) {
	}
}
