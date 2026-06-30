// 동행 신고 사유 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.domain.companion.repository;

import com.sopt.nearby.domain.common.repository.DomainRepository;
import com.sopt.nearby.domain.companion.model.CompanionReportReason;

public interface CompanionReportReasonRepository
		extends DomainRepository<CompanionReportReason, CompanionReportReason.Key> {
}
