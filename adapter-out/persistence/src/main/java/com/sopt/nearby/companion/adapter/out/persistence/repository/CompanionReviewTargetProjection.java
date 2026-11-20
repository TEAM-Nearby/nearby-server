// 동행 후기 대상 목록 쿼리 결과를 담는 Projection
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public interface CompanionReviewTargetProjection {

	Long getRevieweeUserId();

	String getProfileImageUrl();

	String getNickname();

	String getPlaceAddress();

	BigDecimal getLatitude();

	BigDecimal getLongitude();

	LocalDateTime getMeetingAt();

	Boolean getCheckedIn();

	Boolean getHasWrittenReview();
}
