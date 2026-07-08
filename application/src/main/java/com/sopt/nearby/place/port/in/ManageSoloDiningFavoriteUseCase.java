// 혼밥 맛집 즐겨찾기 등록과 해제 유스케이스를 정의한다.
package com.sopt.nearby.place.port.in;

import com.sopt.nearby.place.application.SoloDiningFavoriteCommand;
import com.sopt.nearby.place.application.SoloDiningFavoriteResult;

public interface ManageSoloDiningFavoriteUseCase {

    SoloDiningFavoriteResult register(SoloDiningFavoriteCommand command);

    SoloDiningFavoriteResult remove(SoloDiningFavoriteCommand command);
}
