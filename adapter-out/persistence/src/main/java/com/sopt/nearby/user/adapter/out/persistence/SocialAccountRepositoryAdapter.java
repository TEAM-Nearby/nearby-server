// 소셜 계정 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.user.adapter.out.persistence.entity.SocialAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.sopt.nearby.user.adapter.out.persistence.repository.SocialAccountJpaRepository;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.exception.SocialAccountAlreadyExistsException;
import com.sopt.nearby.user.port.out.SocialAccountRepository;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class SocialAccountRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<SocialAccount, Long, SocialAccountEntity, Long>
		implements SocialAccountRepository {

	private final SocialAccountJpaRepository jpaRepository;

	public SocialAccountRepositoryAdapter(final SocialAccountJpaRepository jpaRepository) {
		super(jpaRepository, UserPersistenceMapper::toEntity, UserPersistenceMapper::toDomain, Function.identity());
		this.jpaRepository = jpaRepository;
	}

	@Override
	public SocialAccount save(final SocialAccount model) {
		try {
			return UserPersistenceMapper.toDomain(jpaRepository.saveAndFlush(UserPersistenceMapper.toEntity(model)));
		} catch (DataIntegrityViolationException exception) {
			throw new SocialAccountAlreadyExistsException(exception);
		}
	}

	@Override
	public Optional<SocialAccount> findByProviderAndProviderUserId(
			final String provider,
			final String providerUserId
	) {
		return jpaRepository.findByProviderAndProviderUserId(provider, providerUserId)
				.map(UserPersistenceMapper::toDomain);
	}
}
