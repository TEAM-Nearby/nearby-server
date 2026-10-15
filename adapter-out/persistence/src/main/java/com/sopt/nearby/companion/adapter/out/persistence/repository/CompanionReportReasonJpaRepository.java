// 동행 신고 사유 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReportReasonEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReportReasonEntityId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionReportReasonJpaRepository
		extends JpaRepository<CompanionReportReasonEntity, CompanionReportReasonEntityId> {
}
