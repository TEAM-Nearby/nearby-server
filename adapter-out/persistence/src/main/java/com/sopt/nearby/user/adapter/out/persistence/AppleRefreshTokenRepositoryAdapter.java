// Apple Refresh Token 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.user.adapter.out.persistence.entity.AppleRefreshTokenEntity;
import com.sopt.nearby.user.adapter.out.persistence.repository.AppleRefreshTokenJpaRepository;
import com.sopt.nearby.user.domain.model.AppleRefreshToken;
import com.sopt.nearby.user.port.out.AppleRefreshTokenRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AppleRefreshTokenRepositoryAdapter implements AppleRefreshTokenRepository {

	private final AppleRefreshTokenJpaRepository jpaRepository;

	public AppleRefreshTokenRepositoryAdapter(final AppleRefreshTokenJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public AppleRefreshToken save(final AppleRefreshToken token) {
		return toDomain(jpaRepository.save(new AppleRefreshTokenEntity(
				token.userId(), token.refreshToken(), token.updatedAt()
		)));
	}

	@Override
	public Optional<AppleRefreshToken> findByUserId(final Long userId) {
		return jpaRepository.findById(userId).map(this::toDomain);
	}

	@Override
	public void deleteByUserId(final Long userId) {
		jpaRepository.deleteById(userId);
	}

	private AppleRefreshToken toDomain(final AppleRefreshTokenEntity entity) {
		return new AppleRefreshToken(entity.getUserId(), entity.getRefreshToken(), entity.getUpdatedAt());
	}
}
