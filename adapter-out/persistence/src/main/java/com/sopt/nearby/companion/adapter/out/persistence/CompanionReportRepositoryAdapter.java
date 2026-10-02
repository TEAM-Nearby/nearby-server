// 동행 신고 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReportEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReportJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.report.CompanionReport;
import com.sopt.nearby.companion.port.out.CompanionReportRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionReportRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionReport, Long, CompanionReportEntity, Long>
		implements CompanionReportRepository {

	public CompanionReportRepositoryAdapter(final CompanionReportJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
		this.jpaRepository = jpaRepository;
	}

	private final CompanionReportJpaRepository jpaRepository;

	@Override
	public boolean existsByMeetingIdAndReporterUserIdAndReportedUserId(
			final Long meetingId,
			final Long reporterUserId,
			final Long reportedUserId
	) {
		return jpaRepository.existsByMeetingIdAndReporterUserIdAndReportedUserId(
				meetingId,
				reporterUserId,
				reportedUserId
		);
	}
}
