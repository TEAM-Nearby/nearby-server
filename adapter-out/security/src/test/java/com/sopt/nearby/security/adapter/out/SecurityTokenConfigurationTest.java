// 보안 토큰 설정의 JWT secret 검증 동작을 확인하는 테스트
package com.sopt.nearby.security.adapter.out;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SecurityTokenConfigurationTest {

	@Test
	void rejectsShortJwtSecret() {
		SecurityTokenConfiguration configuration = new SecurityTokenConfiguration();

		assertThatThrownBy(() -> configuration.jwtEncoder("short"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("nearby.jwt.secret은 HS256 서명을 위해 32바이트 이상이어야 합니다.");
	}
}
