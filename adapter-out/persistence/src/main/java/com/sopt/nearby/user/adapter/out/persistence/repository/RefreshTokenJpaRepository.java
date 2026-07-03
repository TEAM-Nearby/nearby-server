// 리프레시 토큰 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.user.adapter.out.persistence.repository;

import com.sopt.nearby.user.adapter.out.persistence.entity.RefreshTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

	Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
}
