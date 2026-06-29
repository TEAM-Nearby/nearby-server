// 동행 매칭 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.domain.companion.repository;

import com.sopt.nearby.domain.common.repository.DomainRepository;
import com.sopt.nearby.domain.companion.model.CompanionMatch;

public interface CompanionMatchRepository extends DomainRepository<CompanionMatch, Long> {
}
