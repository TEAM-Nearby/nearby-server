// 동행 모집글 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

	@Enumerated(EnumType.STRING)
	@Column(name = "meeting_time_type", nullable = false)
	private CompanionPostMeetingTimeType meetingTimeType;

	@Column(name = "meeting_at")
	private LocalDateTime meetingAt;

	@Column(name = "exposure_expires_at")
	private LocalDateTime exposureExpiresAt;

	@Column(name = "max_participants", nullable = false)
	private int maxParticipants;

	@Column(name = "depart_even_if_not_full", nullable = false)
	private boolean departEvenIfNotFull;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "open_chat_url", nullable = false)
	private String openChatUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CompanionPostStatus status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	protected CompanionPostEntity() {
	}

	public CompanionPostEntity(
			final Long id,
			final Long hostUserId,
			final Long placeId,
			final CompanionPostMeetingTimeType meetingTimeType,
			final LocalDateTime meetingAt,
			final LocalDateTime exposureExpiresAt,
			final int maxParticipants,
			final boolean departEvenIfNotFull,
			final String content,
			final String openChatUrl,
			final CompanionPostStatus status,
			final LocalDateTime createdAt
	) {
		this.id = id;
		this.hostUserId = hostUserId;
		this.placeId = placeId;
		this.meetingTimeType = meetingTimeType;
		this.meetingAt = meetingAt;
		this.exposureExpiresAt = exposureExpiresAt;
		this.maxParticipants = maxParticipants;
		this.departEvenIfNotFull = departEvenIfNotFull;
		this.content = content;
		this.openChatUrl = openChatUrl;
		this.status = status;
		this.createdAt = createdAt;
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
		this(
				id,
				hostUserId,
				placeId,
				CompanionPostMeetingTimeType.SCHEDULED,
				meetingAt,
				null,
				maxParticipants,
				true,
				content,
				openChatUrl,
				status,
				createdAt
		);
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

	public CompanionPostMeetingTimeType getMeetingTimeType() {
		return meetingTimeType;
	}

	public LocalDateTime getMeetingAt() {
		return meetingAt;
	}

	public LocalDateTime getExposureExpiresAt() {
		return exposureExpiresAt;
	}

	public int getMaxParticipants() {
		return maxParticipants;
	}

	public boolean isDepartEvenIfNotFull() {
		return departEvenIfNotFull;
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
