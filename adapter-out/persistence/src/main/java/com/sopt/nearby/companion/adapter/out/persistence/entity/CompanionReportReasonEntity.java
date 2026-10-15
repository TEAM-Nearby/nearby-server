// 동행 신고 사유 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.report.ReportReason;
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
@Table(name = "companion_report_reason")
@IdClass(CompanionReportReasonEntityId.class)
public class CompanionReportReasonEntity {

	@Id
	@Column(name = "report_id", nullable = false)
	private Long reportId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReportReason reason;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "report_id", insertable = false, updatable = false)
	private CompanionReportEntity report;

	protected CompanionReportReasonEntity() {
	}

	public CompanionReportReasonEntity(final Long reportId, final ReportReason reason) {
		this.reportId = reportId;
		this.reason = reason;
	}

	public Long getReportId() {
		return reportId;
	}

	public ReportReason getReason() {
		return reason;
	}
}
