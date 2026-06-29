// 회원 계정 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.domain.user.repository;

import com.sopt.nearby.domain.common.repository.DomainRepository;
import com.sopt.nearby.domain.user.model.UserAccount;

public interface UserAccountRepository extends DomainRepository<UserAccount, Long> {
}
