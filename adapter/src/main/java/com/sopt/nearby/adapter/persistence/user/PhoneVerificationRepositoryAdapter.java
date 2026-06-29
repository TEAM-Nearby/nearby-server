// 휴대폰 인증 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.user;

import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.adapter.persistence.user.entity.PhoneVerificationEntity;
import com.sopt.nearby.adapter.persistence.user.mapper.UserPersistenceMapper;
import com.sopt.nearby.adapter.persistence.user.repository.PhoneVerificationJpaRepository;
import com.sopt.nearby.domain.user.model.PhoneVerification;
import com.sopt.nearby.domain.user.repository.PhoneVerificationRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class PhoneVerificationRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<PhoneVerification, Long, PhoneVerificationEntity, Long>
		implements PhoneVerificationRepository {

	public PhoneVerificationRepositoryAdapter(final PhoneVerificationJpaRepository jpaRepository) {
		super(jpaRepository, UserPersistenceMapper::toEntity, UserPersistenceMapper::toDomain, Function.identity());
	}
}
