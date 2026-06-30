// 동행 신고 사유 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionReportReasonEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionReportReasonEntityId;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionReportReasonJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionReportReason;
import com.sopt.nearby.domain.companion.repository.CompanionReportReasonRepository;
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
