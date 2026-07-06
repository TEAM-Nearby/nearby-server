// 리프레시 토큰 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.user.adapter.out.persistence.repository;

import com.sopt.nearby.user.adapter.out.persistence.entity.RefreshTokenEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

	Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update RefreshTokenEntity refreshToken
			set refreshToken.revokedAt = :revokedAt
			where refreshToken.tokenHash = :tokenHash
				and refreshToken.userId = :userId
				and refreshToken.revokedAt is null
			""")
	int revokeByTokenHashIfActive(
			@Param("tokenHash") String tokenHash,
			@Param("userId") Long userId,
			@Param("revokedAt") LocalDateTime revokedAt
	);
}
