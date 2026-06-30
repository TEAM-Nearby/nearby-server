// 동행 신청 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.CompanionApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "companion_application")
public class CompanionApplicationEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "applicant_user_id", nullable = false)
	private Long applicantUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CompanionApplicationStatus status;

	@Lob
	@Column(name = "rejection_reason")
	private String rejectionReason;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", insertable = false, updatable = false)
	private CompanionPostEntity post;

	protected CompanionApplicationEntity() {
	}

	public CompanionApplicationEntity(
			final Long id,
			final Long postId,
			final Long applicantUserId,
			final CompanionApplicationStatus status,
			final String rejectionReason,
			final LocalDateTime createdAt
	) {
		this.id = id;
		this.postId = postId;
		this.applicantUserId = applicantUserId;
		this.status = status;
		this.rejectionReason = rejectionReason;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Long getPostId() {
		return postId;
	}

	public Long getApplicantUserId() {
		return applicantUserId;
	}

	public CompanionApplicationStatus getStatus() {
		return status;
	}

	public String getRejectionReason() {
		return rejectionReason;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
