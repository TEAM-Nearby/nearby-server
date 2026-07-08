// 혼밥 맛집 즐겨찾기 목록 조회 유스케이스를 정의한다.
package com.sopt.nearby.place.port.in;

import com.sopt.nearby.place.application.ReadSoloDiningFavoritesCommand;
import com.sopt.nearby.place.application.SoloDiningFavoritesResult;

public interface ReadSoloDiningFavoritesUseCase {

    SoloDiningFavoritesResult read(ReadSoloDiningFavoritesCommand command);
}
