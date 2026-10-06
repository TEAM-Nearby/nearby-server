// 액세스 토큰 사용자의 활성 상태 검증 동작을 확인하는 테스트
package com.sopt.nearby.security.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class ActiveUserJwtValidatorTest {

	@Test
	void acceptsActiveUser() {
		ActiveUserJwtValidator validator = new ActiveUserJwtValidator(userId -> true);

		assertThat(validator.validate(jwt("7")).hasErrors()).isFalse();
	}

	@Test
	void rejectsWithdrawnUser() {
		ActiveUserJwtValidator validator = new ActiveUserJwtValidator(userId -> false);

		assertThat(validator.validate(jwt("7")).hasErrors()).isTrue();
	}

	private Jwt jwt(final String subject) {
		return Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.subject(subject)
				.issuedAt(Instant.parse("2026-10-06T00:00:00Z"))
				.expiresAt(Instant.parse("2026-10-06T01:00:00Z"))
				.build();
	}

}
