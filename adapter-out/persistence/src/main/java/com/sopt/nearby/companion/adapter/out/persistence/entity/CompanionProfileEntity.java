// 동행 프로필 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.UserGender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "companion_profile")
public class CompanionProfileEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserGender gender;

	@Column(name = "birth_year")
	private Integer birthYear;

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	private String intro;

	@Column(name = "manner_score", nullable = false, precision = 3, scale = 2)
	private BigDecimal mannerScore;

	@Column(name = "review_count", nullable = false)
	private int reviewCount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CompanionProfileStatus status;

	protected CompanionProfileEntity() {
	}

	public CompanionProfileEntity(
			final Long id,
			final Long userId,
			final String nickname,
			final UserGender gender,
			final Integer birthYear,
			final String profileImageUrl,
			final String intro,
			final BigDecimal mannerScore,
			final int reviewCount,
			final CompanionProfileStatus status
	) {
		this.id = id;
		this.userId = userId;
		this.nickname = nickname;
		this.gender = gender;
		this.birthYear = birthYear;
		this.profileImageUrl = profileImageUrl;
		this.intro = intro;
		this.mannerScore = mannerScore;
		this.reviewCount = reviewCount;
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public String getNickname() {
		return nickname;
	}

	public UserGender getGender() {
		return gender;
	}

	public Integer getBirthYear() {
		return birthYear;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

	public String getIntro() {
		return intro;
	}

	public BigDecimal getMannerScore() {
		return mannerScore;
	}

	public int getReviewCount() {
		return reviewCount;
	}

	public CompanionProfileStatus getStatus() {
		return status;
	}
}
