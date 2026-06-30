// 휴대폰 인증 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.adapter.persistence.user.entity;

import com.sopt.nearby.domain.user.model.PhoneVerificationStatus;
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
@Table(name = "phone_verification")
public class PhoneVerificationEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "phone_number", nullable = false)
	private String phoneNumber;

	private String carrier;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PhoneVerificationStatus status;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Column(name = "verified_at")
	private LocalDateTime verifiedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private UserAccountEntity user;

	protected PhoneVerificationEntity() {
	}

	public PhoneVerificationEntity(
			final Long id,
			final Long userId,
			final String phoneNumber,
			final String carrier,
			final PhoneVerificationStatus status,
			final LocalDateTime expiresAt,
			final LocalDateTime verifiedAt
	) {
		this.id = id;
		this.userId = userId;
		this.phoneNumber = phoneNumber;
		this.carrier = carrier;
		this.status = status;
		this.expiresAt = expiresAt;
		this.verifiedAt = verifiedAt;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getCarrier() {
		return carrier;
	}

	public PhoneVerificationStatus getStatus() {
		return status;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public LocalDateTime getVerifiedAt() {
		return verifiedAt;
	}
}
