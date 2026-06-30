// 도메인 저장소 포트의 최소 저장과 식별자 조회 계약을 정의하는 인터페이스
package com.sopt.nearby.domain.common.repository;

import java.util.Optional;

public interface DomainRepository<T, ID> {

	T save(T model);

	Optional<T> findById(ID id);
}
