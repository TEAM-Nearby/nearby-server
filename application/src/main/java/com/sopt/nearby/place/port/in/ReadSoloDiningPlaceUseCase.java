// 혼밥 맛집 상세 조회 유스케이스 계약이다.
package com.sopt.nearby.place.port.in;

import com.sopt.nearby.place.application.ReadSoloDiningPlaceCommand;
import com.sopt.nearby.place.application.SoloDiningPlaceResult;

public interface ReadSoloDiningPlaceUseCase {

    SoloDiningPlaceResult read(ReadSoloDiningPlaceCommand command);
}
