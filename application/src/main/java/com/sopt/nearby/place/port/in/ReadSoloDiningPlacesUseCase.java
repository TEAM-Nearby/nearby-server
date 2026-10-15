// 혼밥 맛집 목록 조회 유스케이스 계약이다.
package com.sopt.nearby.place.port.in;

import com.sopt.nearby.place.application.ReadSoloDiningPlacesCommand;
import com.sopt.nearby.place.application.SoloDiningPlacesResult;

public interface ReadSoloDiningPlacesUseCase {

    SoloDiningPlacesResult read(ReadSoloDiningPlacesCommand command);
}
