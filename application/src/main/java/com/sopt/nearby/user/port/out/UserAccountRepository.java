// 회원 계정 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.user.port.out;

import com.sopt.nearby.common.port.DomainRepository;
import com.sopt.nearby.user.domain.model.UserAccount;

public interface UserAccountRepository extends DomainRepository<UserAccount, Long> {
}
