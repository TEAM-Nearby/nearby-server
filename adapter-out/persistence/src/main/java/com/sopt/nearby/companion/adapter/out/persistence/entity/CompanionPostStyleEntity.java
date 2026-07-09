// 동행 모집글 성향 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.post.CompanionPostKeyword;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "companion_post_style")
@IdClass(CompanionPostStyleEntityId.class)
public class CompanionPostStyleEntity {

	@Id
	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CompanionPostKeyword keyword;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", insertable = false, updatable = false)
	private CompanionPostEntity post;

	protected CompanionPostStyleEntity() {
	}

	public CompanionPostStyleEntity(final Long postId, final CompanionPostKeyword keyword) {
		this.postId = postId;
		this.keyword = keyword;
	}

	public Long getPostId() {
		return postId;
	}

	public CompanionPostKeyword getKeyword() {
		return keyword;
	}
}
