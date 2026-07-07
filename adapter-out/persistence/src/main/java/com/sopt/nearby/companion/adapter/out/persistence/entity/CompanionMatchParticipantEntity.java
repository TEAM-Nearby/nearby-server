// 동행 매칭 참여자 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
		name = "companion_match_participant",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_companion_match_participant_match_user",
				columnNames = {"match_id", "user_id"}
		)
)
public class CompanionMatchParticipantEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "match_id", nullable = false)
	private Long matchId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "accepted_application_id")
	private Long acceptedApplicationId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MatchParticipantRole role;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "match_id", insertable = false, updatable = false)
	private CompanionMatchEntity match;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "accepted_application_id", insertable = false, updatable = false)
	private CompanionApplicationEntity acceptedApplication;

	protected CompanionMatchParticipantEntity() {
	}

	public CompanionMatchParticipantEntity(
			final Long id,
			final Long matchId,
			final Long userId,
			final Long acceptedApplicationId,
			final MatchParticipantRole role
	) {
		this.id = id;
		this.matchId = matchId;
		this.userId = userId;
		this.acceptedApplicationId = acceptedApplicationId;
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public Long getMatchId() {
		return matchId;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getAcceptedApplicationId() {
		return acceptedApplicationId;
	}

	public MatchParticipantRole getRole() {
		return role;
	}
}
