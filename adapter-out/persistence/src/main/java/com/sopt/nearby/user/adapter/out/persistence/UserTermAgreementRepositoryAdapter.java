// 회원 약관 동의 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.user.adapter.out.persistence.entity.UserTermAgreementEntity;
import com.sopt.nearby.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.sopt.nearby.user.adapter.out.persistence.repository.UserTermAgreementJpaRepository;
import com.sopt.nearby.user.domain.model.UserTermAgreement;
import com.sopt.nearby.user.port.out.UserTermAgreementRepository;
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
