// 리프레시 토큰 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.user.adapter.out.persistence.entity.RefreshTokenEntity;
import com.sopt.nearby.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.sopt.nearby.user.adapter.out.persistence.repository.RefreshTokenJpaRepository;
import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RefreshTokenRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<RefreshToken, Long, RefreshTokenEntity, Long>
		implements RefreshTokenRepository {

	private final RefreshTokenJpaRepository jpaRepository;

	public RefreshTokenRepositoryAdapter(final RefreshTokenJpaRepository jpaRepository) {
		super(jpaRepository, UserPersistenceMapper::toEntity, UserPersistenceMapper::toDomain, Function.identity());
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<RefreshToken> findByTokenHash(final String tokenHash) {
		return jpaRepository.findByTokenHash(tokenHash)
				.map(UserPersistenceMapper::toDomain);
	}

	@Override
	@Transactional
	public boolean revokeByTokenHashIfActive(
			final String tokenHash,
			final Long userId,
			final LocalDateTime revokedAt
	) {
		return jpaRepository.revokeByTokenHashIfActive(tokenHash, userId, revokedAt) > 0;
	}
}
