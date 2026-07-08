// 동행 리뷰 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewJpaRepository;
import com.sopt.nearby.companion.domain.exception.CompanionReviewAlreadyExistsException;
import com.sopt.nearby.companion.domain.model.review.CompanionReview;
import com.sopt.nearby.companion.port.out.CompanionReviewRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionReviewRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionReview, Long, CompanionReviewEntity, Long>
		implements CompanionReviewRepository {

	private static final String REVIEW_UNIQUE_CONSTRAINT_NAME = "uk_companion_review_meeting_reviewer_reviewee";

	private final CompanionReviewJpaRepository jpaRepository;

	public CompanionReviewRepositoryAdapter(final CompanionReviewJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
		this.jpaRepository = jpaRepository;
	}

	@Override
	public CompanionReview save(final CompanionReview model) {
		CompanionReviewEntity entity = CompanionPersistenceMapper.toEntity(model);
		CompanionReviewEntity savedEntity;
		try {
			savedEntity = jpaRepository.saveAndFlush(entity);
		} catch (DataIntegrityViolationException exception) {
			throw mapUniqueConstraintViolation(exception);
		}
		return CompanionPersistenceMapper.toDomain(savedEntity);
	}

	@Override
	public boolean existsByMeetingIdAndReviewerUserIdAndRevieweeUserId(
			final Long meetingId,
			final Long reviewerUserId,
			final Long revieweeUserId
	) {
		return jpaRepository.existsByMeetingIdAndReviewerUserIdAndRevieweeUserId(
				meetingId,
				reviewerUserId,
				revieweeUserId
		);
	}

	private RuntimeException mapUniqueConstraintViolation(final DataIntegrityViolationException exception) {
		if (isReviewUniqueConstraintViolation(exception)) {
			return new CompanionReviewAlreadyExistsException();
		}
		return exception;
	}

	private boolean isReviewUniqueConstraintViolation(final DataIntegrityViolationException exception) {
		String normalizedMessage = String.valueOf(exception.getMessage()).toLowerCase(Locale.ROOT);
		return normalizedMessage.contains(REVIEW_UNIQUE_CONSTRAINT_NAME)
				|| (normalizedMessage.contains("unique")
				&& normalizedMessage.contains("companion_review")
				&& normalizedMessage.contains("meeting_id")
				&& normalizedMessage.contains("reviewer_user_id")
				&& normalizedMessage.contains("reviewee_user_id"));
	}
}
