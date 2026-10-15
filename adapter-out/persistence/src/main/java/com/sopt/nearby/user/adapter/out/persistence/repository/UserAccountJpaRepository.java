// 회원 계정 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.user.adapter.out.persistence.repository;

import com.sopt.nearby.user.adapter.out.persistence.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from UserAccountEntity u where u.id = :id")
	Optional<UserAccountEntity> findByIdForUpdate(Long id);
}
