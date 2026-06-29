// 약관 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.user;

import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.adapter.persistence.user.entity.TermEntity;
import com.sopt.nearby.adapter.persistence.user.mapper.UserPersistenceMapper;
import com.sopt.nearby.adapter.persistence.user.repository.TermJpaRepository;
import com.sopt.nearby.domain.user.model.Term;
import com.sopt.nearby.domain.user.repository.TermRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class TermRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<Term, Long, TermEntity, Long>
		implements TermRepository {

	public TermRepositoryAdapter(final TermJpaRepository jpaRepository) {
		super(jpaRepository, UserPersistenceMapper::toEntity, UserPersistenceMapper::toDomain, Function.identity());
	}
}
