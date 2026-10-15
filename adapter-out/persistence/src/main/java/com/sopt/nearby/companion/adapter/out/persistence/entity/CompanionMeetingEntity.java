// 동행 미팅 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
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
@Table(name = "companion_meeting")
public class CompanionMeetingEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "match_id", nullable = false)
	private Long matchId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CompanionMeetingStatus status;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "match_id", insertable = false, updatable = false)
	private CompanionMatchEntity match;

	protected CompanionMeetingEntity() {
	}

	public CompanionMeetingEntity(
			final Long id,
			final Long matchId,
			final CompanionMeetingStatus status,
			final LocalDateTime startedAt,
			final LocalDateTime completedAt
	) {
		this.id = id;
		this.matchId = matchId;
		this.status = status;
		this.startedAt = startedAt;
		this.completedAt = completedAt;
	}

	public Long getId() {
		return id;
	}

	public Long getMatchId() {
		return matchId;
	}

	public CompanionMeetingStatus getStatus() {
		return status;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}
}
