// 약관 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.adapter.persistence.user.repository;

import com.sopt.nearby.adapter.persistence.user.entity.TermEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermJpaRepository extends JpaRepository<TermEntity, Long> {
}
