// 동행 신고 테이블을 매핑하는 JPA 엔티티
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
		name = "companion_report",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_companion_report_match_reporter_reported",
				columnNames = {"match_id", "reporter_user_id", "reported_user_id"}
		)
)
public class CompanionReportEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "match_id", nullable = false)
	private Long matchId;

	@Column(name = "meeting_id")
	private Long meetingId;

	@Column(name = "reporter_user_id", nullable = false)
	private Long reporterUserId;

	@Column(name = "reported_user_id", nullable = false)
	private Long reportedUserId;

	@Column(columnDefinition = "text")
	private String detail;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "meeting_id", insertable = false, updatable = false)
	private CompanionMeetingEntity meeting;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "match_id", insertable = false, updatable = false)
	private CompanionMatchEntity match;

	protected CompanionReportEntity() {
	}

	public CompanionReportEntity(
			final Long id,
			final Long matchId,
			final Long meetingId,
			final Long reporterUserId,
			final Long reportedUserId,
			final String detail,
			final LocalDateTime createdAt
	) {
		this.id = id;
		this.matchId = matchId;
		this.meetingId = meetingId;
		this.reporterUserId = reporterUserId;
		this.reportedUserId = reportedUserId;
		this.detail = detail;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Long getMeetingId() {
		return meetingId;
	}

	public Long getMatchId() {
		return matchId;
	}

	public Long getReporterUserId() {
		return reporterUserId;
	}

	public Long getReportedUserId() {
		return reportedUserId;
	}

	public String getDetail() {
		return detail;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
