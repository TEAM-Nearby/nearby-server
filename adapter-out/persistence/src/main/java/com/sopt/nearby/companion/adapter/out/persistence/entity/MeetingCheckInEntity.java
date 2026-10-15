// 미팅 체크인 테이블을 매핑하는 JPA 엔티티
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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
		name = "meeting_check_in",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_meeting_check_in_meeting_user",
				columnNames = {"meeting_id", "user_id"}
		)
)
public class MeetingCheckInEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "meeting_id", nullable = false)
	private Long meetingId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, precision = 11, scale = 8)
	private BigDecimal latitude;

	@Column(nullable = false, precision = 11, scale = 8)
	private BigDecimal longitude;

	@Column(name = "checked_in_at", nullable = false)
	private LocalDateTime checkedInAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "meeting_id", insertable = false, updatable = false)
	private CompanionMeetingEntity meeting;

	protected MeetingCheckInEntity() {
	}

	public MeetingCheckInEntity(
			final Long id,
			final Long meetingId,
			final Long userId,
			final BigDecimal latitude,
			final BigDecimal longitude,
			final LocalDateTime checkedInAt,
			final LocalDateTime completedAt
	) {
		this.id = id;
		this.meetingId = meetingId;
		this.userId = userId;
		this.latitude = latitude;
		this.longitude = longitude;
		this.checkedInAt = checkedInAt;
		this.completedAt = completedAt;
	}

	public Long getId() {
		return id;
	}

	public Long getMeetingId() {
		return meetingId;
	}

	public Long getUserId() {
		return userId;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public LocalDateTime getCheckedInAt() {
		return checkedInAt;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}
}
