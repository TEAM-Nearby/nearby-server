// Apple Refresh Token 저장소를 추상화하는 포트
package com.sopt.nearby.user.port.out;

import com.sopt.nearby.user.domain.model.AppleRefreshToken;
import java.util.Optional;

public interface AppleRefreshTokenRepository {

	AppleRefreshToken save(AppleRefreshToken token);

	Optional<AppleRefreshToken> findByUserId(Long userId);

	void deleteByUserId(Long userId);
}
