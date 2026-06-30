// 동행 매칭 참여자 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.common.port.DomainRepository;
import com.sopt.nearby.companion.domain.model.CompanionMatchParticipant;

public interface CompanionMatchParticipantRepository extends DomainRepository<CompanionMatchParticipant, Long> {
}
