// 동행 모집글 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.common.port.DomainRepository;
import com.sopt.nearby.companion.domain.model.CompanionPost;

public interface CompanionPostRepository extends DomainRepository<CompanionPost, Long> {
}
