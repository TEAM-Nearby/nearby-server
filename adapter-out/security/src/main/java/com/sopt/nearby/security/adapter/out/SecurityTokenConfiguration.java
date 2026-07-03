// 액세스 토큰과 카카오 ID 토큰 검증에 필요한 보안 토큰 빈을 구성한다
package com.sopt.nearby.security.adapter.out;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class SecurityTokenConfiguration {

	private static final int HS256_MIN_SECRET_BYTES = 32;

	@Bean
	public JwtEncoder jwtEncoder(
			@Value("${nearby.jwt.secret}")
			final String secret
	) {
		SecretKey secretKey = secretKey(secret);
		return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey.getEncoded()));
	}

	@Bean
	public JwtDecoder accessTokenJwtDecoder(
			@Value("${nearby.jwt.secret}")
			final String secret
	) {
		return NimbusJwtDecoder.withSecretKey(secretKey(secret))
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
	}

	@Bean
	public JwtDecoder kakaoJwtDecoder(
			@Value("${kakao.oidc.issuer-uri:https://kauth.kakao.com}") final String issuerUri,
			@Value("${kakao.oidc.jwk-set-uri:https://kauth.kakao.com/.well-known/jwks.json}") final String jwkSetUri
	) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
		return decoder;
	}

	private SecretKey secretKey(final String secret) {
		byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < HS256_MIN_SECRET_BYTES) {
			throw new IllegalStateException("nearby.jwt.secret은 HS256 서명을 위해 32바이트 이상이어야 합니다.");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}
}
