// 동행 일정 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.adapter.persistence.companion.entity;

import com.sopt.nearby.adapter.persistence.place.entity.PlaceCacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "companion_schedule")
public class CompanionScheduleEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "match_id", nullable = false)
	private Long matchId;

	@Column(name = "place_id", nullable = false)
	private Long placeId;

	@Column(name = "scheduled_at", nullable = false)
	private LocalDateTime scheduledAt;

	@Column(name = "estimated_duration_minutes")
	private Integer estimatedDurationMinutes;

	@Column(nullable = false)
	private boolean confirmed;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "match_id", insertable = false, updatable = false)
	private CompanionMatchEntity match;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "place_id", insertable = false, updatable = false)
	private PlaceCacheEntity place;

	protected CompanionScheduleEntity() {
	}

	public CompanionScheduleEntity(
			final Long id,
			final Long matchId,
			final Long placeId,
			final LocalDateTime scheduledAt,
			final Integer estimatedDurationMinutes,
			final boolean confirmed
	) {
		this.id = id;
		this.matchId = matchId;
		this.placeId = placeId;
		this.scheduledAt = scheduledAt;
		this.estimatedDurationMinutes = estimatedDurationMinutes;
		this.confirmed = confirmed;
	}

	public Long getId() {
		return id;
	}

	public Long getMatchId() {
		return matchId;
	}

	public Long getPlaceId() {
		return placeId;
	}

	public LocalDateTime getScheduledAt() {
		return scheduledAt;
	}

	public Integer getEstimatedDurationMinutes() {
		return estimatedDurationMinutes;
	}

	public boolean isConfirmed() {
		return confirmed;
	}
}
