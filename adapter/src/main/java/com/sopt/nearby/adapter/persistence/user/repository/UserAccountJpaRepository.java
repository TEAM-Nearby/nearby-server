// 회원 계정 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.adapter.persistence.user.repository;

import com.sopt.nearby.adapter.persistence.user.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, Long> {
}
