// Nearby 액세스 토큰의 사용자가 현재 활성 상태인지 검증하는 validator
package com.sopt.nearby.security.adapter.out;

import com.sopt.nearby.user.port.in.CheckActiveUserUseCase;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class ActiveUserJwtValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error INVALID_USER = new OAuth2Error(
			"invalid_token",
			"활성 상태가 아닌 회원의 액세스 토큰입니다.",
			null
	);

	private final CheckActiveUserUseCase checkActiveUserUseCase;

	public ActiveUserJwtValidator(final CheckActiveUserUseCase checkActiveUserUseCase) {
		this.checkActiveUserUseCase = checkActiveUserUseCase;
	}

	@Override
	public OAuth2TokenValidatorResult validate(final Jwt token) {
		try {
			Long userId = Long.valueOf(token.getSubject());
			boolean active = checkActiveUserUseCase.isActive(userId);
			return active
					? OAuth2TokenValidatorResult.success()
					: OAuth2TokenValidatorResult.failure(INVALID_USER);
		} catch (RuntimeException exception) {
			return OAuth2TokenValidatorResult.failure(INVALID_USER);
		}
	}
}
