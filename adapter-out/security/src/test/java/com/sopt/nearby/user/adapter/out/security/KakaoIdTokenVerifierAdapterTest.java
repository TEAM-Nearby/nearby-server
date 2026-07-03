// 카카오 ID 토큰 검증 어댑터의 클레임 검증 동작을 확인하는 테스트
package com.sopt.nearby.user.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.user.application.VerifiedKakaoUser;
import com.sopt.nearby.user.exception.KakaoLoginFailedException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class KakaoIdTokenVerifierAdapterTest {

	@Test
	void returnsKakaoSubjectWhenAudienceAndNonceMatch() {
		KakaoIdTokenVerifierAdapter adapter = new KakaoIdTokenVerifierAdapter(
				token -> jwt("kakao-subject", "native-app-key", "nonce"),
				"native-app-key"
		);

		VerifiedKakaoUser user = adapter.verify("id-token", "nonce");

		assertThat(user.providerUserId()).isEqualTo("kakao-subject");
	}

	@Test
	void failsWhenNonceDoesNotMatch() {
		KakaoIdTokenVerifierAdapter adapter = new KakaoIdTokenVerifierAdapter(
				token -> jwt("kakao-subject", "native-app-key", "other-nonce"),
				"native-app-key"
		);

		assertThatThrownBy(() -> adapter.verify("id-token", "nonce"))
				.isInstanceOf(KakaoLoginFailedException.class);
	}

	@Test
	void failsWhenAudienceDoesNotMatch() {
		KakaoIdTokenVerifierAdapter adapter = new KakaoIdTokenVerifierAdapter(
				token -> jwt("kakao-subject", "other-key", "nonce"),
				"native-app-key"
		);

		assertThatThrownBy(() -> adapter.verify("id-token", "nonce"))
				.isInstanceOf(KakaoLoginFailedException.class);
	}

	@Test
	void failsWhenDecoderRejectsToken() {
		JwtDecoder decoder = token -> {
			throw new BadJwtException("bad token");
		};
		KakaoIdTokenVerifierAdapter adapter = new KakaoIdTokenVerifierAdapter(decoder, "native-app-key");

		assertThatThrownBy(() -> adapter.verify("bad-token", "nonce"))
				.isInstanceOf(KakaoLoginFailedException.class);
	}

	private Jwt jwt(final String subject, final String audience, final String nonce) {
		return Jwt.withTokenValue("id-token")
				.header("alg", "RS256")
				.subject(subject)
				.audience(List.of(audience))
				.claim("nonce", nonce)
				.issuedAt(Instant.parse("2026-07-03T12:00:00Z"))
				.expiresAt(Instant.parse("2026-07-03T13:00:00Z"))
				.build();
	}
}
