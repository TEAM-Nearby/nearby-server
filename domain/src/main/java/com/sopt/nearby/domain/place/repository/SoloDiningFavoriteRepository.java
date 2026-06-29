// 혼밥 장소 즐겨찾기 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.domain.place.repository;

import com.sopt.nearby.domain.common.repository.DomainRepository;
import com.sopt.nearby.domain.place.model.SoloDiningFavorite;

public interface SoloDiningFavoriteRepository extends DomainRepository<SoloDiningFavorite, Long> {
}
