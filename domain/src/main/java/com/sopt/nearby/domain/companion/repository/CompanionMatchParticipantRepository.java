// 동행 매칭 참여자 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.domain.companion.repository;

import com.sopt.nearby.domain.common.repository.DomainRepository;
import com.sopt.nearby.domain.companion.model.CompanionMatchParticipant;

public interface CompanionMatchParticipantRepository extends DomainRepository<CompanionMatchParticipant, Long> {
}
