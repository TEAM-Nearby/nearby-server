// 동행 모집글 성향 엔티티의 복합 키를 표현하는 JPA 식별자 클래스
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.post.CompanionPostKeyword;
import java.io.Serializable;
import java.util.Objects;

public class CompanionPostStyleEntityId implements Serializable {

	private Long postId;
	private CompanionPostKeyword keyword;

	public CompanionPostStyleEntityId() {
	}

	public CompanionPostStyleEntityId(final Long postId, final CompanionPostKeyword keyword) {
		this.postId = postId;
		this.keyword = keyword;
	}

	public Long getPostId() {
		return postId;
	}

	public CompanionPostKeyword getKeyword() {
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
