// 동행 신고 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionReportEntity;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionReportJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionReport;
import com.sopt.nearby.domain.companion.repository.CompanionReportRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionReportRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionReport, Long, CompanionReportEntity, Long>
		implements CompanionReportRepository {

	public CompanionReportRepositoryAdapter(final CompanionReportJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
	}
}
