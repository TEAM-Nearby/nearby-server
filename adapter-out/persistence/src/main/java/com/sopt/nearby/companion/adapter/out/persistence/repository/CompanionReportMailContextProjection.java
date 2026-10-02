// 동행 신고 이메일 조회 쿼리의 결과를 표현하는 Projection
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.time.LocalDateTime;

public interface CompanionReportMailContextProjection {

	String getReporterNickname();

	String getReporterPhoneNumber();

	String getReportedNickname();

	String getReportedPhoneNumber();

	Long getCompanionPostId();

	String getCompanionPostContent();

	LocalDateTime getScheduledAt();

	String getPlaceAddress();
}
