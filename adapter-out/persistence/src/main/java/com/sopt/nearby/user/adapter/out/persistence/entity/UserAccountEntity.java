// 회원 계정 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.user.adapter.out.persistence.entity;

import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
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
@Table(name = "user_account")
public class UserAccountEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserAccountStatus status;

	@Column(name = "phone_number")
	private String phoneNumber;

	@Column(name = "phone_verified_at")
	private LocalDateTime phoneVerifiedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "onboarding_status", nullable = false)
	private UserOnboardingStatus onboardingStatus;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	protected UserAccountEntity() {
	}

	public UserAccountEntity(
			final Long id,
			final UserRole role,
			final UserAccountStatus status,
			final String phoneNumber,
			final LocalDateTime phoneVerifiedAt,
			final UserOnboardingStatus onboardingStatus,
			final LocalDateTime createdAt,
			final LocalDateTime deletedAt
	) {
		this.id = id;
		this.role = role;
		this.status = status;
		this.phoneNumber = phoneNumber;
		this.phoneVerifiedAt = phoneVerifiedAt;
		this.onboardingStatus = onboardingStatus;
		this.createdAt = createdAt;
		this.deletedAt = deletedAt;
	}

	public Long getId() {
		return id;
	}

	public UserRole getRole() {
		return role;
	}

	public UserAccountStatus getStatus() {
		return status;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public LocalDateTime getPhoneVerifiedAt() {
		return phoneVerifiedAt;
	}

	public UserOnboardingStatus getOnboardingStatus() {
		return onboardingStatus;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getDeletedAt() {
		return deletedAt;
	}
}
