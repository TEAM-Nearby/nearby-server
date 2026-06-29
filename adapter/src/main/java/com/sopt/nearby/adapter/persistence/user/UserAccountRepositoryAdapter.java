// 회원 계정 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.user;

import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.adapter.persistence.user.entity.UserAccountEntity;
import com.sopt.nearby.adapter.persistence.user.mapper.UserPersistenceMapper;
import com.sopt.nearby.adapter.persistence.user.repository.UserAccountJpaRepository;
import com.sopt.nearby.domain.user.model.UserAccount;
import com.sopt.nearby.domain.user.repository.UserAccountRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class UserAccountRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<UserAccount, Long, UserAccountEntity, Long>
		implements UserAccountRepository {

	public UserAccountRepositoryAdapter(final UserAccountJpaRepository jpaRepository) {
		super(jpaRepository, UserPersistenceMapper::toEntity, UserPersistenceMapper::toDomain, Function.identity());
	}
}
