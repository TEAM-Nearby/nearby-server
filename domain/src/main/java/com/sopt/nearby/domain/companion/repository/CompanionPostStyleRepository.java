// 동행 모집글 성향 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.domain.companion.repository;

import com.sopt.nearby.domain.common.repository.DomainRepository;
import com.sopt.nearby.domain.companion.model.CompanionPostStyle;

public interface CompanionPostStyleRepository extends DomainRepository<CompanionPostStyle, CompanionPostStyle.Key> {
}
