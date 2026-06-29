// 동행 모집글 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.adapter.persistence.companion.entity;

import com.sopt.nearby.adapter.persistence.place.entity.PlaceCacheEntity;
import com.sopt.nearby.adapter.persistence.user.entity.UserAccountEntity;
import com.sopt.nearby.domain.companion.model.CompanionPostStatus;
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
@Table(name = "companion_post")
public class CompanionPostEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "host_user_id", nullable = false)
	private Long hostUserId;

	@Column(name = "place_id", nullable = false)
	private Long placeId;

	@Column(name = "meeting_at", nullable = false)
	private LocalDateTime meetingAt;

	@Column(name = "max_participants", nullable = false)
	private int maxParticipants;

	@Lob
	@Column(nullable = false)
	private String content;

	@Column(name = "open_chat_url", nullable = false)
	private String openChatUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CompanionPostStatus status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "host_user_id", insertable = false, updatable = false)
	private UserAccountEntity hostUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "place_id", insertable = false, updatable = false)
	private PlaceCacheEntity place;

	protected CompanionPostEntity() {
	}

	public CompanionPostEntity(
			final Long id,
			final Long hostUserId,
			final Long placeId,
			final LocalDateTime meetingAt,
			final int maxParticipants,
			final String content,
			final String openChatUrl,
			final CompanionPostStatus status,
			final LocalDateTime createdAt
	) {
		this.id = id;
		this.hostUserId = hostUserId;
		this.placeId = placeId;
		this.meetingAt = meetingAt;
		this.maxParticipants = maxParticipants;
		this.content = content;
		this.openChatUrl = openChatUrl;
		this.status = status;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Long getHostUserId() {
		return hostUserId;
	}

	public Long getPlaceId() {
		return placeId;
	}

	public LocalDateTime getMeetingAt() {
		return meetingAt;
	}

	public int getMaxParticipants() {
		return maxParticipants;
	}

	public String getContent() {
		return content;
	}

	public String getOpenChatUrl() {
		return openChatUrl;
	}

	public CompanionPostStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
