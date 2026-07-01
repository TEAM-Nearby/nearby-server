// 혼밥 장소 즐겨찾기 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.place.port.out;

import com.sopt.nearby.common.port.DomainRepository;
import com.sopt.nearby.place.domain.model.SoloDiningFavorite;

public interface SoloDiningFavoriteRepository extends DomainRepository<SoloDiningFavorite, Long> {
}
