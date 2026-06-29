// 동행 신고 사유 엔티티의 복합 키를 표현하는 JPA 식별자 클래스
package com.sopt.nearby.adapter.persistence.companion.entity;

import com.sopt.nearby.domain.companion.model.ReportReason;
import java.io.Serializable;
import java.util.Objects;

public class CompanionReportReasonEntityId implements Serializable {

	private Long reportId;
	private ReportReason reason;

	protected CompanionReportReasonEntityId() {
	}

	public CompanionReportReasonEntityId(final Long reportId, final ReportReason reason) {
		this.reportId = reportId;
		this.reason = reason;
	}

	public Long getReportId() {
		return reportId;
	}

	public ReportReason getReason() {
		return reason;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CompanionReportReasonEntityId that)) {
			return false;
		}
		return Objects.equals(reportId, that.reportId) && reason == that.reason;
	}

	@Override
	public int hashCode() {
		return Objects.hash(reportId, reason);
	}
}
