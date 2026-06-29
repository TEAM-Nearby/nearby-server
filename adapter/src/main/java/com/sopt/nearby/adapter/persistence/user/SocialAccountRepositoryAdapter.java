// 소셜 계정 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.user;

import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.adapter.persistence.user.entity.SocialAccountEntity;
import com.sopt.nearby.adapter.persistence.user.mapper.UserPersistenceMapper;
import com.sopt.nearby.adapter.persistence.user.repository.SocialAccountJpaRepository;
import com.sopt.nearby.domain.user.model.SocialAccount;
import com.sopt.nearby.domain.user.repository.SocialAccountRepository;
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
