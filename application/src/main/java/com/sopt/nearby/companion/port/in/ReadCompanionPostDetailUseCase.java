// 동행 모집 글 상세 조회 유스케이스를 정의한다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.CompanionPostDetailResult;
import com.sopt.nearby.companion.application.ReadCompanionPostDetailCommand;

public interface ReadCompanionPostDetailUseCase {

    CompanionPostDetailResult read(ReadCompanionPostDetailCommand command);
}
