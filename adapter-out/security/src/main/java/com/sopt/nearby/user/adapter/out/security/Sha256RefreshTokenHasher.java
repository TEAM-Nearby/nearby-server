// 원본 리프레시 토큰을 SHA-256 해시로 변환하는 보안 어댑터
package com.sopt.nearby.user.adapter.out.security;

import com.sopt.nearby.user.port.out.RefreshTokenHasher;
import org.springframework.stereotype.Component;

@Component
public class Sha256RefreshTokenHasher implements RefreshTokenHasher {

	@Override
	public String hash(final String refreshToken) {
		return RefreshTokenHashSupport.sha256(refreshToken);
	}
}
