// 동행 프로필 상세 조회용 쿼리 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.profile.CompanionProfileDetail;
import java.util.Optional;

public interface CompanionProfileDetailQueryPort {

    Optional<CompanionProfileDetail> findByProfileId(Long profileId);
}
