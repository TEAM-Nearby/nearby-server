// 동행 신고에 연결된 사유를 표현하는 도메인 모델
package com.sopt.nearby.domain.companion.model;

public record CompanionReportReason(
		Long reportId,
		ReportReason reason
) {

	public Key key() {
		return new Key(reportId, reason);
	}

	public record Key(Long reportId, ReportReason reason) {
	}
}
