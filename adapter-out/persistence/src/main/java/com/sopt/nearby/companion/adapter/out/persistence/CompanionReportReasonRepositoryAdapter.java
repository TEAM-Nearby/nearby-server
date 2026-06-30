// 동행 신고 사유 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReportReasonEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReportReasonEntityId;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReportReasonJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.CompanionReportReason;
import com.sopt.nearby.companion.port.out.CompanionReportReasonRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionReportReasonRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionReportReason, CompanionReportReason.Key,
				CompanionReportReasonEntity, CompanionReportReasonEntityId>
		implements CompanionReportReasonRepository {

	public CompanionReportReasonRepositoryAdapter(final CompanionReportReasonJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				CompanionPersistenceMapper::toEntityId);
	}
}
