// 공급자별 회원 탈퇴 진행 상태 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.user.adapter.out.persistence.entity.WithdrawalProgressEntity;
import com.sopt.nearby.user.adapter.out.persistence.repository.WithdrawalProgressJpaRepository;
import com.sopt.nearby.user.domain.model.WithdrawalProgress;
import com.sopt.nearby.user.port.out.WithdrawalProgressRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class WithdrawalProgressRepositoryAdapter implements WithdrawalProgressRepository {
	private final WithdrawalProgressJpaRepository jpaRepository;

	public WithdrawalProgressRepositoryAdapter(final WithdrawalProgressJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public WithdrawalProgress save(final WithdrawalProgress progress) {
		return jpaRepository.save(new WithdrawalProgressEntity(progress)).toDomain();
	}

	@Override
	public Optional<WithdrawalProgress> findByUserIdAndProvider(final Long userId, final String provider) {
		return jpaRepository.findById(userId + ":" + provider).map(WithdrawalProgressEntity::toDomain);
	}

	@Override
	public List<WithdrawalProgress> findAllByUserId(final Long userId) {
		return jpaRepository.findAllByUserId(userId).stream().map(WithdrawalProgressEntity::toDomain).toList();
	}

	@Override
	public List<Long> findResumableUserIds(final int limit) {
		return jpaRepository.findResumableUserIds(limit);
	}

	@Override
	public void deleteByUserId(final Long userId) {
		jpaRepository.deleteByUserId(userId);
	}
}
