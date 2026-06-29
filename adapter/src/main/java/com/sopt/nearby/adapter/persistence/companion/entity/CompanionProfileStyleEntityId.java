// 동행 프로필 성향 엔티티의 복합 키를 표현하는 JPA 식별자 클래스
package com.sopt.nearby.adapter.persistence.companion.entity;

import com.sopt.nearby.domain.companion.model.TravelStyleKeyword;
import java.io.Serializable;
import java.util.Objects;

public class CompanionProfileStyleEntityId implements Serializable {

	private Long profileId;
	private TravelStyleKeyword keyword;

	protected CompanionProfileStyleEntityId() {
	}

	public CompanionProfileStyleEntityId(final Long profileId, final TravelStyleKeyword keyword) {
		this.profileId = profileId;
		this.keyword = keyword;
	}

	public Long getProfileId() {
		return profileId;
	}

	public TravelStyleKeyword getKeyword() {
		return keyword;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CompanionProfileStyleEntityId that)) {
			return false;
		}
		return Objects.equals(profileId, that.profileId) && keyword == that.keyword;
	}

	@Override
	public int hashCode() {
		return Objects.hash(profileId, keyword);
	}
}
