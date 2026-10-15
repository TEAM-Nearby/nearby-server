// 혼밥 장소 즐겨찾기 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.place.adapter.out.persistence.entity;

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
import java.time.LocalDateTime;

@Entity
@Table(
		name = "solo_dining_favorite",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_solo_dining_favorite_user_place",
				columnNames = {"user_id", "place_id"}
		)
)
public class SoloDiningFavoriteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "place_id", nullable = false)
	private Long placeId;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "place_id", insertable = false, updatable = false)
	private PlaceCacheEntity place;

	protected SoloDiningFavoriteEntity() {
	}

	public SoloDiningFavoriteEntity(
			final Long id,
			final Long userId,
			final Long placeId,
			final LocalDateTime createdAt
	) {
		this.id = id;
		this.userId = userId;
		this.placeId = placeId;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getPlaceId() {
		return placeId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
