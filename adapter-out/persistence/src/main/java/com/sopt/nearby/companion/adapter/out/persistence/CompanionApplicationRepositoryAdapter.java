// 동행 신청 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionApplicationJpaRepository;
import com.sopt.nearby.companion.domain.exception.CompanionRequestAlreadyExistsException;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.match.CompanionApplication;
import com.sopt.nearby.companion.port.out.CompanionApplicationRepository;
import java.sql.SQLException;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionApplicationRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionApplication, Long, CompanionApplicationEntity, Long>
		implements CompanionApplicationRepository {

	private static final String APPLICATION_UNIQUE_CONSTRAINT_NAME = "uk_companion_application_post_applicant";
	private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

	private final CompanionApplicationJpaRepository jpaRepository;

	public CompanionApplicationRepositoryAdapter(final CompanionApplicationJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
		this.jpaRepository = jpaRepository;
	}

	@Override
	public CompanionApplication save(final CompanionApplication model) {
		try {
			return CompanionPersistenceMapper.toDomain(
					jpaRepository.saveAndFlush(CompanionPersistenceMapper.toEntity(model))
			);
		} catch (DataIntegrityViolationException exception) {
			throw mapUniqueConstraintViolation(exception);
		}
	}

	@Override
	public boolean existsByPostIdAndApplicantUserId(final Long postId, final Long applicantUserId) {
		return jpaRepository.existsByPostIdAndApplicantUserId(postId, applicantUserId);
	}

	private RuntimeException mapUniqueConstraintViolation(final DataIntegrityViolationException exception) {
		if (isApplicationUniqueConstraintViolation(exception)) {
			return new CompanionRequestAlreadyExistsException();
		}
		return exception;
	}

	private boolean isApplicationUniqueConstraintViolation(final DataIntegrityViolationException exception) {
		return hasSqlState(exception, UNIQUE_VIOLATION_SQL_STATE)
				|| hasApplicationUniqueConstraintMessage(exception);
	}

	private boolean hasSqlState(final Throwable exception, final String sqlState) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof SQLException sqlException && sqlState.equals(sqlException.getSQLState())) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private boolean hasApplicationUniqueConstraintMessage(final Throwable exception) {
		String normalizedMessage = String.valueOf(exception.getMessage()).toLowerCase(Locale.ROOT);
		return normalizedMessage.contains(APPLICATION_UNIQUE_CONSTRAINT_NAME)
				|| (normalizedMessage.contains("unique")
				&& normalizedMessage.contains("companion_application")
				&& normalizedMessage.contains("post_id")
				&& normalizedMessage.contains("applicant_user_id"));
	}
}
