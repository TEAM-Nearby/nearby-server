// 애플 ID 토큰 검증 어댑터의 클레임 검증 동작을 확인하는 테스트
package com.sopt.nearby.user.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.user.application.VerifiedUser;
import com.sopt.nearby.user.exception.AppleLoginFailedException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class AppleIdTokenVerifierAdapterTest {

	@Test
	void returnsAppleSubjectWhenAudienceAndNonceMatch() {
		AppleIdTokenVerifierAdapter adapter = new AppleIdTokenVerifierAdapter(
				token -> jwt("apple-subject", "apple-client-id", "nonce"),
				"apple-client-id"
		);

		VerifiedUser user = adapter.verify("id-token", "nonce");

		assertThat(user.providerUserId()).isEqualTo("apple-subject");
	}

	@Test
	void failsWhenNonceDoesNotMatch() {
		AppleIdTokenVerifierAdapter adapter = new AppleIdTokenVerifierAdapter(
				token -> jwt("apple-subject", "apple-client-id", "other-nonce"),
				"apple-client-id"
		);

		assertThatThrownBy(() -> adapter.verify("id-token", "nonce"))
				.isInstanceOf(AppleLoginFailedException.class);
	}

	@Test
	void failsWhenAudienceDoesNotMatch() {
		AppleIdTokenVerifierAdapter adapter = new AppleIdTokenVerifierAdapter(
				token -> jwt("apple-subject", "other-client-id", "nonce"),
				"apple-client-id"
		);

		assertThatThrownBy(() -> adapter.verify("id-token", "nonce"))
				.isInstanceOf(AppleLoginFailedException.class);
	}

	@Test
	void failsWhenDecoderRejectsToken() {
		JwtDecoder decoder = token -> {
			throw new BadJwtException("bad token");
		};
		AppleIdTokenVerifierAdapter adapter = new AppleIdTokenVerifierAdapter(decoder, "apple-client-id");

		assertThatThrownBy(() -> adapter.verify("bad-token", "nonce"))
				.isInstanceOf(AppleLoginFailedException.class);
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
