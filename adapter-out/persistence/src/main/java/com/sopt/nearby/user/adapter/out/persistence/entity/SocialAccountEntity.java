// 소셜 계정 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.user.adapter.out.persistence.entity;

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

@Entity
@Table(
		name = "social_account",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_social_account_provider_user",
				columnNames = {"provider", "provider_user_id"}
		)
)
public class SocialAccountEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false)
	private String provider;

	@Column(name = "provider_user_id", nullable = false)
	private String providerUserId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private UserAccountEntity user;

	protected SocialAccountEntity() {
	}

	public SocialAccountEntity(final Long id, final Long userId, final String provider, final String providerUserId) {
		this.id = id;
		this.userId = userId;
		this.provider = provider;
		this.providerUserId = providerUserId;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public String getProvider() {
		return provider;
	}

	public String getProviderUserId() {
		return providerUserId;
	}
}
