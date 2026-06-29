// 동행 매칭 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.adapter.persistence.companion.entity;

import com.sopt.nearby.domain.companion.model.CompanionMatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "companion_match")
public class CompanionMatchEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CompanionMatchStatus status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", insertable = false, updatable = false)
	private CompanionPostEntity post;

	protected CompanionMatchEntity() {
	}

	public CompanionMatchEntity(
			final Long id,
			final Long postId,
			final CompanionMatchStatus status,
			final LocalDateTime createdAt
	) {
		this.id = id;
		this.postId = postId;
		this.status = status;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Long getPostId() {
		return postId;
	}

	public CompanionMatchStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
