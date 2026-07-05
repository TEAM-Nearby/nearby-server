// 동행 프로필 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.companion.domain.exception.DuplicateCompanionProfileException;
import com.sopt.nearby.companion.domain.exception.DuplicateNicknameException;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionProfileRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionProfile, Long, CompanionProfileEntity, Long>
		implements CompanionProfileRepository {

	private final CompanionProfileJpaRepository jpaRepository;

	public CompanionProfileRepositoryAdapter(final CompanionProfileJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
		this.jpaRepository = jpaRepository;
	}

	@Override
	public CompanionProfile save(final CompanionProfile model) {
		try {
			return CompanionPersistenceMapper.toDomain(
					jpaRepository.saveAndFlush(CompanionPersistenceMapper.toEntity(model))
			);
		} catch (DataIntegrityViolationException exception) {
			throw mapUniqueConstraintViolation(exception);
		}
	}

	@Override
	public List<CompanionProfile> findAllByUserIdIn(final List<Long> userIds) {
		return jpaRepository.findAllByUserIdIn(userIds)
				.stream()
				.map(CompanionPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsByNickname(final String nickname) {
		return jpaRepository.existsByNickname(nickname);
	}

	@Override
	public boolean existsByUserId(final Long userId) {
		return jpaRepository.existsByUserId(userId);
	}

	@Override
	public Optional<CompanionProfile> findByUserId(final Long userId) {
		return jpaRepository.findByUserId(userId).map(CompanionPersistenceMapper::toDomain);
	}

	private RuntimeException mapUniqueConstraintViolation(final DataIntegrityViolationException exception) {
		String normalizedConstraint = constraintMessage(exception).toLowerCase();
		if (normalizedConstraint.contains("companion_profile_nickname")) {
			return new DuplicateNicknameException();
		}
		if (normalizedConstraint.contains("companion_profile_user")) {
			return new DuplicateCompanionProfileException();
		}
		return exception;
	}

	private String constraintMessage(final DataIntegrityViolationException exception) {
		String message = exception.getMessage();
		if (message == null) {
			return "";
		}
		int index = message.lastIndexOf("constraint [");
		return index < 0 ? message : message.substring(index);
	}
}
