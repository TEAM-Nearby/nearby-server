// 동행 리뷰 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
		name = "companion_review",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_companion_review_meeting_reviewer_reviewee",
				columnNames = {"meeting_id", "reviewer_user_id", "reviewee_user_id"}
		)
)
public class CompanionReviewEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "meeting_id", nullable = false)
	private Long meetingId;

	@Column(name = "reviewer_user_id", nullable = false)
	private Long reviewerUserId;

	@Column(name = "reviewee_user_id", nullable = false)
	private Long revieweeUserId;

	@Column(nullable = false)
	private int rating;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "meeting_id", insertable = false, updatable = false)
	private CompanionMeetingEntity meeting;

	protected CompanionReviewEntity() {
	}

	public CompanionReviewEntity(
			final Long id,
			final Long meetingId,
			final Long reviewerUserId,
			final Long revieweeUserId,
			final int rating,
			final LocalDateTime createdAt
	) {
		this.id = id;
		this.meetingId = meetingId;
		this.reviewerUserId = reviewerUserId;
		this.revieweeUserId = revieweeUserId;
		this.rating = rating;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Long getMeetingId() {
		return meetingId;
	}

	public Long getReviewerUserId() {
		return reviewerUserId;
	}

	public Long getRevieweeUserId() {
		return revieweeUserId;
	}

	public int getRating() {
		return rating;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
