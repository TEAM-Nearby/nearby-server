// 리프레시 토큰 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.user.port.out;

import com.sopt.nearby.user.domain.model.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository {

	RefreshToken save(RefreshToken model);

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	boolean revokeByTokenHashIfActive(String tokenHash, Long userId, LocalDateTime revokedAt);
}
