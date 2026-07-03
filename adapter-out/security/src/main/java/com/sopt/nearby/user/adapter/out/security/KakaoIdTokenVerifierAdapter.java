// 카카오 OIDC ID 토큰을 검증해 카카오 사용자 식별자를 반환하는 어댑터
package com.sopt.nearby.user.adapter.out.security;

import com.sopt.nearby.user.application.VerifiedKakaoUser;
import com.sopt.nearby.user.exception.KakaoLoginFailedException;
import com.sopt.nearby.user.port.out.KakaoIdTokenVerifier;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
public class KakaoIdTokenVerifierAdapter implements KakaoIdTokenVerifier {

	private final JwtDecoder jwtDecoder;
	private final String nativeAppKey;

	public KakaoIdTokenVerifierAdapter(
			@Qualifier("kakaoJwtDecoder") final JwtDecoder jwtDecoder,
			@Value("${kakao.native-app-key:${KAKAO_NATIVE_APP_KEY:}}") final String nativeAppKey
	) {
		this.jwtDecoder = jwtDecoder;
		this.nativeAppKey = nativeAppKey;
	}

	@Override
	public VerifiedKakaoUser verify(final String idToken, final String nonce) {
		if (isBlank(nativeAppKey)) {
			throw new KakaoLoginFailedException();
		}

		try {
			Jwt jwt = jwtDecoder.decode(idToken);
			validateAudience(jwt.getAudience());
			validateNonce(jwt.getClaimAsString("nonce"), nonce);
			validateSubject(jwt.getSubject());
			return new VerifiedKakaoUser(jwt.getSubject());
		} catch (JwtException exception) {
			throw new KakaoLoginFailedException();
		}
	}

	private void validateAudience(final List<String> audience) {
		if (audience == null || !audience.contains(nativeAppKey)) {
			throw new KakaoLoginFailedException();
		}
	}

	private void validateNonce(final String tokenNonce, final String expectedNonce) {
		if (!expectedNonce.equals(tokenNonce)) {
			throw new KakaoLoginFailedException();
		}
	}

	private void validateSubject(final String subject) {
		if (isBlank(subject)) {
			throw new KakaoLoginFailedException();
		}
	}

	private boolean isBlank(final String value) {
		return value == null || value.isBlank();
	}
}
