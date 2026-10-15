// JPA 저장소를 도메인 저장소 포트에 연결하는 공통 어댑터 기반 클래스
package com.sopt.nearby.shared.adapter.out.persistence.support;

import com.sopt.nearby.common.port.DomainRepository;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class SimpleJpaRepositoryAdapter<T, ID, E, JPA_ID> implements DomainRepository<T, ID> {

	private final JpaRepository<E, JPA_ID> jpaRepository;
	private final Function<T, E> toEntity;
	private final Function<E, T> toDomain;
	private final Function<ID, JPA_ID> toJpaId;

	protected SimpleJpaRepositoryAdapter(
			final JpaRepository<E, JPA_ID> jpaRepository,
			final Function<T, E> toEntity,
			final Function<E, T> toDomain,
			final Function<ID, JPA_ID> toJpaId
	) {
		this.jpaRepository = jpaRepository;
		this.toEntity = toEntity;
		this.toDomain = toDomain;
		this.toJpaId = toJpaId;
	}

	@Override
	public T save(final T model) {
		return toDomain.apply(jpaRepository.save(toEntity.apply(model)));
	}

	@Override
	public Optional<T> findById(final ID id) {
		return jpaRepository.findById(toJpaId.apply(id)).map(toDomain);
	}
}
