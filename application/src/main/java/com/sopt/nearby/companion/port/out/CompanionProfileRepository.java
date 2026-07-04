// 동행 프로필 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.common.port.DomainRepository;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import java.util.List;

public interface CompanionProfileRepository extends DomainRepository<CompanionProfile, Long> {
    List<CompanionProfile> findAllByUserIdIn(List<Long> list);
}
