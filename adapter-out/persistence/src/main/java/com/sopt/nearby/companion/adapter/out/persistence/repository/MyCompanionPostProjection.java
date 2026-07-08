// 내가 작성한 동행 모집글 목록 네이티브 쿼리 결과를 받는 projection이다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MyCompanionPostProjection {

	Long getPostId();

	LocalDateTime getScheduledAt();

	String getGooglePlaceId();

	String getPlaceName();

	String getPlaceAddress();

	BigDecimal getLatitude();

	BigDecimal getLongitude();

	Number getCurrentParticipants();

	Number getMaxParticipants();

	String getContent();
}
