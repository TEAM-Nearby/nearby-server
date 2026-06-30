// 소셜 계정 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.user.adapter.out.persistence.entity.SocialAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.sopt.nearby.user.adapter.out.persistence.repository.SocialAccountJpaRepository;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.port.out.SocialAccountRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class SocialAccountRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<SocialAccount, Long, SocialAccountEntity, Long>
		implements SocialAccountRepository {

	public SocialAccountRepositoryAdapter(final SocialAccountJpaRepository jpaRepository) {
		super(jpaRepository, UserPersistenceMapper::toEntity, UserPersistenceMapper::toDomain, Function.identity());
	}
}
