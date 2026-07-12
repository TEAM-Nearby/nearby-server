// 수락된 동행 신청 상세 조회 결과를 담는 Projection
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface AcceptedCompanionRequestDetailProjection {

    Long getMatchId();

    String getMatchStatus();

    Long getPostId();

    Long getHostUserId();

    String getHostNickname();

    String getHostProfileImageUrl();

    String getGooglePlaceId();

    String getPlaceName();

    String getPlaceAddress();

    BigDecimal getPlaceLatitude();

    BigDecimal getPlaceLongitude();

    String getMeetingTimeType();

    LocalDateTime getMeetingAt();

    Integer getParticipantCount();

    Integer getMaxParticipants();

    String getOpenChatUrl();
}
