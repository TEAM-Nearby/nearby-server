// 매칭된 목록을 가져오는 UseCase
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.ReadCompanionMatchResult;
import java.util.List;

public interface ReadCompanionMatchesUseCase {
    List<ReadCompanionMatchResult> getMatches(Long userId);
}
