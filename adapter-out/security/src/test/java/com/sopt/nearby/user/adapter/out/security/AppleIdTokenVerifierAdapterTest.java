// 애플 ID 토큰 검증 어댑터의 클레임 검증 동작을 확인하는 테스트
package com.sopt.nearby.user.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.user.application.VerifiedUser;
import com.sopt.nearby.user.exception.AppleClientIdNotConfiguredException;
import com.sopt.nearby.user.exception.AppleIdTokenAudienceMismatchException;
import com.sopt.nearby.user.exception.AppleIdTokenNonceMismatchException;
import com.sopt.nearby.user.exception.AppleIdTokenSubjectMissingException;
import com.sopt.nearby.user.exception.AppleIdTokenVerificationFailedException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class AppleIdTokenVerifierAdapterTest {
	private static final String HASHED_NONCE = "78377b525757b494427f89014f97d79928f3938d14eb51e20fb5dec9834eb304";
	private static final String OTHER_HASHED_NONCE = "59bcb2470d7a22b8a9f227d96aaf80645e61d5055aa187c1374ed78333d10765";

	@Test
	void returnsAppleSubjectWhenAudienceAndHashedNonceMatch() {
		AppleIdTokenVerifierAdapter adapter = new AppleIdTokenVerifierAdapter(
				token -> jwt("apple-subject", "apple-client-id", HASHED_NONCE),
				"apple-client-id"
		);

		VerifiedUser user = adapter.verify("id-token", "nonce");

		assertThat(user.providerUserId()).isEqualTo("apple-subject");
	}

	@Test
	void failsWhenNonceDoesNotMatch() {
		AppleIdTokenVerifierAdapter adapter = new AppleIdTokenVerifierAdapter(
				token -> jwt("apple-subject", "apple-client-id", OTHER_HASHED_NONCE),
				"apple-client-id"
		);

		assertThatThrownBy(() -> adapter.verify("id-token", "nonce"))
				.isExactlyInstanceOf(AppleIdTokenNonceMismatchException.class);
	}

	@Test
	void failsWhenAudienceDoesNotMatch() {
		AppleIdTokenVerifierAdapter adapter = new AppleIdTokenVerifierAdapter(
				token -> jwt("apple-subject", "other-client-id", HASHED_NONCE),
				"apple-client-id"
		);

		assertThatThrownBy(() -> adapter.verify("id-token", "nonce"))
				.isExactlyInstanceOf(AppleIdTokenAudienceMismatchException.class);
	}

	@Test
	void failsWhenDecoderRejectsToken() {
		JwtDecoder decoder = token -> {
			throw new BadJwtException("bad token");
		};
		AppleIdTokenVerifierAdapter adapter = new AppleIdTokenVerifierAdapter(decoder, "apple-client-id");

		assertThatThrownBy(() -> adapter.verify("bad-token", "nonce"))
				.isExactlyInstanceOf(AppleIdTokenVerificationFailedException.class);
	}

	@Test
	void failsWhenClientIdIsNotConfigured() {
		AppleIdTokenVerifierAdapter adapter = new AppleIdTokenVerifierAdapter(
				token -> jwt("apple-subject", "apple-client-id", HASHED_NONCE),
				""
		);

		assertThatThrownBy(() -> adapter.verify("id-token", "nonce"))
				.isExactlyInstanceOf(AppleClientIdNotConfiguredException.class);
	}

	@Test
	void failsWhenSubjectIsMissing() {
		AppleIdTokenVerifierAdapter adapter = new AppleIdTokenVerifierAdapter(
				token -> jwt("", "apple-client-id", HASHED_NONCE),
				"apple-client-id"
		);

		assertThatThrownBy(() -> adapter.verify("id-token", "nonce"))
				.isExactlyInstanceOf(AppleIdTokenSubjectMissingException.class);
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
