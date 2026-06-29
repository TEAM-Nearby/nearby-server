// 긴급 연락처 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.adapter.persistence.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "emergency_contact")
public class EmergencyContactEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false)
	private String name;

	@Column(name = "phone_number", nullable = false)
	private String phoneNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private UserAccountEntity user;

	protected EmergencyContactEntity() {
	}

	public EmergencyContactEntity(final Long id, final Long userId, final String name, final String phoneNumber) {
		this.id = id;
		this.userId = userId;
		this.name = name;
		this.phoneNumber = phoneNumber;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public String getName() {
		return name;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}
}
