// 회원 약관 동의 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.domain.user.repository;

import com.sopt.nearby.domain.common.repository.DomainRepository;
import com.sopt.nearby.domain.user.model.UserTermAgreement;

public interface UserTermAgreementRepository extends DomainRepository<UserTermAgreement, Long> {
}
