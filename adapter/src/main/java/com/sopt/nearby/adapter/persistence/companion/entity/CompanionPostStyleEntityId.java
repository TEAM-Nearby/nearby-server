// 동행 모집글 성향 엔티티의 복합 키를 표현하는 JPA 식별자 클래스
package com.sopt.nearby.adapter.persistence.companion.entity;

import com.sopt.nearby.domain.companion.model.TravelStyleKeyword;
import java.io.Serializable;
import java.util.Objects;

public class CompanionPostStyleEntityId implements Serializable {

	private Long postId;
	private TravelStyleKeyword keyword;

	public CompanionPostStyleEntityId() {
	}

	public CompanionPostStyleEntityId(final Long postId, final TravelStyleKeyword keyword) {
		this.postId = postId;
		this.keyword = keyword;
	}

	public Long getPostId() {
		return postId;
	}

	public TravelStyleKeyword getKeyword() {
		return keyword;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CompanionPostStyleEntityId that)) {
			return false;
		}
		return Objects.equals(postId, that.postId) && keyword == that.keyword;
	}

	@Override
	public int hashCode() {
		return Objects.hash(postId, keyword);
	}
}
