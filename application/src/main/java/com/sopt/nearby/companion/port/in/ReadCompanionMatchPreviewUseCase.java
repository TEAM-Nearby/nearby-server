//컨트롤러가 호출할 inbound port
//입력은 matchId, 로그인 유저 ID 정도
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;

public interface ReadCompanionMatchPreviewUseCase {
    CompanionMatchPreview getPreview(Long matchId);
}
