// JWT 발급 어댑터의 액세스 토큰 클레임과 리프레시 토큰 해시를 검증하는 테스트
package com.sopt.nearby.user.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sopt.nearby.user.application.IssuedTokens;
import com.sopt.nearby.user.application.TokenIssueRequest;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtTokenAdapterTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2099-01-01T12:00:00Z"), ZoneId.of("UTC"));
	private static final String SECRET = "12345678901234567890123456789012";

	@Test
	void issuesAccessTokenAndHashedRefreshToken() throws Exception {
		SecretKey secretKey = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		JwtTokenAdapter adapter = new JwtTokenAdapter(
				new NimbusJwtEncoder(new ImmutableSecret<>(secretKey.getEncoded())),
				3600,
				1209600,
				CLOCK
		);

		IssuedTokens tokens = adapter.issue(new TokenIssueRequest(
				1L,
				UserRole.USER,
				UserOnboardingStatus.STARTED
		));
		Jwt decodedAccessToken = NimbusJwtDecoder.withSecretKey(secretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build()
				.decode(tokens.accessToken());

		assertThat(decodedAccessToken.getSubject()).isEqualTo("1");
		assertThat(decodedAccessToken.getClaimAsString("role")).isEqualTo("USER");
		assertThat(decodedAccessToken.getClaimAsString("onboardingStatus")).isEqualTo("STARTED");
		assertThat(tokens.refreshToken()).isNotBlank();
		assertThat(tokens.refreshTokenHash()).isEqualTo(sha256(tokens.refreshToken()));
		assertThat(tokens.accessTokenExpiresIn()).isEqualTo(3600);
		assertThat(tokens.refreshTokenExpiresIn()).isEqualTo(1209600);
	}

	private String sha256(final String value) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(digest);
	}
}
