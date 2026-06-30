// 회원 약관 동의 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.user;

import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.adapter.persistence.user.entity.UserTermAgreementEntity;
import com.sopt.nearby.adapter.persistence.user.mapper.UserPersistenceMapper;
import com.sopt.nearby.adapter.persistence.user.repository.UserTermAgreementJpaRepository;
import com.sopt.nearby.domain.user.model.UserTermAgreement;
import com.sopt.nearby.domain.user.repository.UserTermAgreementRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class UserTermAgreementRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<UserTermAgreement, Long, UserTermAgreementEntity, Long>
		implements UserTermAgreementRepository {

	public UserTermAgreementRepositoryAdapter(final UserTermAgreementJpaRepository jpaRepository) {
		super(jpaRepository, UserPersistenceMapper::toEntity, UserPersistenceMapper::toDomain, Function.identity());
	}
}
