// 동행 신고 사유 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.adapter.persistence.companion.repository;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionReportReasonEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionReportReasonEntityId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionReportReasonJpaRepository
		extends JpaRepository<CompanionReportReasonEntity, CompanionReportReasonEntityId> {
}
