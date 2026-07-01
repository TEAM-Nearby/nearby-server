// 동행 프로필 성향 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "companion_profile_style")
@IdClass(CompanionProfileStyleEntityId.class)
public class CompanionProfileStyleEntity {

	@Id
	@Column(name = "profile_id", nullable = false)
	private Long profileId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TravelStyleKeyword keyword;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "profile_id", insertable = false, updatable = false)
	private CompanionProfileEntity profile;

	protected CompanionProfileStyleEntity() {
	}

	public CompanionProfileStyleEntity(final Long profileId, final TravelStyleKeyword keyword) {
		this.profileId = profileId;
		this.keyword = keyword;
	}

	public Long getProfileId() {
		return profileId;
	}

	public TravelStyleKeyword getKeyword() {
		return keyword;
	}
}
