// 동행 신고 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReportEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReportJpaRepository;
import com.sopt.nearby.companion.domain.exception.CompanionReportAlreadyExistsException;
import com.sopt.nearby.companion.domain.model.report.CompanionReport;
import com.sopt.nearby.companion.port.out.CompanionReportRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionReportRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionReport, Long, CompanionReportEntity, Long>
		implements CompanionReportRepository {

	private static final String REPORT_UNIQUE_CONSTRAINT_NAME =
			"uk_companion_report_meeting_reporter_reported";

	public CompanionReportRepositoryAdapter(final CompanionReportJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
		this.jpaRepository = jpaRepository;
	}

	private final CompanionReportJpaRepository jpaRepository;

	@Override
	public CompanionReport save(final CompanionReport model) {
		try {
			return CompanionPersistenceMapper.toDomain(
					jpaRepository.saveAndFlush(CompanionPersistenceMapper.toEntity(model))
			);
		} catch (DataIntegrityViolationException exception) {
			if (isReportUniqueConstraintViolation(exception)) {
				throw new CompanionReportAlreadyExistsException();
			}
			throw exception;
		}
	}

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

	private boolean isReportUniqueConstraintViolation(final DataIntegrityViolationException exception) {
		String normalizedMessage = String.valueOf(exception.getMessage()).toLowerCase(Locale.ROOT);
		return normalizedMessage.contains(REPORT_UNIQUE_CONSTRAINT_NAME)
				|| (normalizedMessage.contains("unique")
				&& normalizedMessage.contains("companion_report")
				&& normalizedMessage.contains("meeting_id")
				&& normalizedMessage.contains("reporter_user_id")
				&& normalizedMessage.contains("reported_user_id"));
	}
}
