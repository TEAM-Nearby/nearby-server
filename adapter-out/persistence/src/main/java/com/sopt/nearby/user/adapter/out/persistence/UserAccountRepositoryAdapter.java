// 회원 계정 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.user.adapter.out.persistence.entity.UserAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.sopt.nearby.user.adapter.out.persistence.repository.UserAccountJpaRepository;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.port.out.UserAccountRepository;
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
