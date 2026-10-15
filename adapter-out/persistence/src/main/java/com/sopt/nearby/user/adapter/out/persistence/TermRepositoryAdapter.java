// 약관 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.user.adapter.out.persistence.entity.TermEntity;
import com.sopt.nearby.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.sopt.nearby.user.adapter.out.persistence.repository.TermJpaRepository;
import com.sopt.nearby.user.domain.model.Term;
import com.sopt.nearby.user.port.out.TermRepository;
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
