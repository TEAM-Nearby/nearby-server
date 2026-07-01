// 리프레시 토큰 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.user.adapter.out.persistence.entity.RefreshTokenEntity;
import com.sopt.nearby.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.sopt.nearby.user.adapter.out.persistence.repository.RefreshTokenJpaRepository;
import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<RefreshToken, Long, RefreshTokenEntity, Long>
		implements RefreshTokenRepository {

	public RefreshTokenRepositoryAdapter(final RefreshTokenJpaRepository jpaRepository) {
		super(jpaRepository, UserPersistenceMapper::toEntity, UserPersistenceMapper::toDomain, Function.identity());
	}
}
