// Apple 계정 연동 해제용 Refresh Token을 저장하는 JPA 엔티티
package com.sopt.nearby.user.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "apple_refresh_token")
public class AppleRefreshTokenEntity {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "refresh_token", nullable = false, columnDefinition = "text")
	private String refreshToken;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected AppleRefreshTokenEntity() {
	}

	public AppleRefreshTokenEntity(final Long userId, final String refreshToken, final LocalDateTime updatedAt) {
		this.userId = userId;
		this.refreshToken = refreshToken;
		this.updatedAt = updatedAt;
	}

	public Long getUserId() {
		return userId;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
