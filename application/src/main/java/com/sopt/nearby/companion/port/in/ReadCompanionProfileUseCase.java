// 동행 프로필 상세 조회 유스케이스 진입점을 정의하는 포트
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.ReadCompanionProfileCommand;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileDetail;

public interface ReadCompanionProfileUseCase {

    CompanionProfileDetail read(ReadCompanionProfileCommand command);
}
