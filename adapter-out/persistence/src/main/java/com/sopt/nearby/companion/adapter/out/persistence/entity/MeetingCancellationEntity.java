// 미팅 취소 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "meeting_cancellation")
public class MeetingCancellationEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "meeting_id", nullable = false)
	private Long meetingId;

	@Column(name = "canceled_by_user_id", nullable = false)
	private Long canceledByUserId;

	@Lob
	@Column(nullable = false)
	private String reason;

	@Column(name = "canceled_at", nullable = false)
	private LocalDateTime canceledAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "meeting_id", insertable = false, updatable = false)
	private CompanionMeetingEntity meeting;

	protected MeetingCancellationEntity() {
	}

	public MeetingCancellationEntity(
			final Long id,
			final Long meetingId,
			final Long canceledByUserId,
			final String reason,
			final LocalDateTime canceledAt
	) {
		this.id = id;
		this.meetingId = meetingId;
		this.canceledByUserId = canceledByUserId;
		this.reason = reason;
		this.canceledAt = canceledAt;
	}

	public Long getId() {
		return id;
	}

	public Long getMeetingId() {
		return meetingId;
	}

	public Long getCanceledByUserId() {
		return canceledByUserId;
	}

	public String getReason() {
		return reason;
	}

	public LocalDateTime getCanceledAt() {
		return canceledAt;
	}
}
