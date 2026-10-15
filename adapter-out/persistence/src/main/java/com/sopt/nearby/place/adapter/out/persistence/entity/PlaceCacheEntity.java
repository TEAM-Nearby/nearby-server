// 장소 캐시 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.place.adapter.out.persistence.entity;

import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
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
@Table(name = "place_cache")
public class PlaceCacheEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "google_place_id", nullable = false, unique = true)
	private String googlePlaceId;

	@Column(nullable = false)
	private String name;

	private String address;

	@Column(nullable = false, precision = 11, scale = 8)
	private BigDecimal latitude;

	@Column(nullable = false, precision = 11, scale = 8)
	private BigDecimal longitude;

	private String category;

	@Column(name = "phone_number")
	private String phoneNumber;

	private BigDecimal rating;

	@Column(name = "review_count")
	private Integer reviewCount;

	@Column(name = "photo_reference", columnDefinition = "text")
	private String photoReference;

	@Enumerated(EnumType.STRING)
	@Column(name = "business_status", nullable = false)
	private PlaceBusinessStatus businessStatus;

	protected PlaceCacheEntity() {
	}

	public PlaceCacheEntity(
			final Long id,
			final String googlePlaceId,
			final String name,
			final String address,
			final BigDecimal latitude,
			final BigDecimal longitude,
			final String category,
			final String phoneNumber,
			final BigDecimal rating,
			final Integer reviewCount,
			final String photoReference,
			final PlaceBusinessStatus businessStatus
	) {
		this.id = id;
		this.googlePlaceId = googlePlaceId;
		this.name = name;
		this.address = address;
		this.latitude = latitude;
		this.longitude = longitude;
		this.category = category;
		this.phoneNumber = phoneNumber;
		this.rating = rating;
		this.reviewCount = reviewCount;
		this.photoReference = photoReference;
		this.businessStatus = businessStatus;
	}

	public Long getId() {
		return id;
	}

	public String getGooglePlaceId() {
		return googlePlaceId;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return address;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public String getCategory() {
		return category;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public BigDecimal getRating() {
		return rating;
	}

	public Integer getReviewCount() {
		return reviewCount;
	}

	public String getPhotoReference() {
		return photoReference;
	}

	public PlaceBusinessStatus getBusinessStatus() {
		return businessStatus;
	}
}
