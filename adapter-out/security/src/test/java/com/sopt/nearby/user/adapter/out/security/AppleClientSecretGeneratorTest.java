// Apple client_secret JWT의 서명과 필수 클레임을 검증하는 테스트
package com.sopt.nearby.user.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AppleClientSecretGeneratorTest {

	@Test
	void generatesSignedClientSecretWithAppleClaims() throws Exception {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
		keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
				+ Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(keyPair.getPrivate().getEncoded())
				+ "\n-----END PRIVATE KEY-----";
		String privateKeyBase64 = Base64.getEncoder()
				.encodeToString(privateKeyPem.getBytes(StandardCharsets.UTF_8));
		Clock clock = Clock.fixed(Instant.parse("2026-10-06T00:00:00Z"), ZoneOffset.UTC);

		String token = new AppleClientSecretGenerator(clock).generate(
				"TEAM_ID", "KEY_ID", "com.dewby.Nearby", privateKeyBase64
		);

		SignedJWT jwt = SignedJWT.parse(token);
		assertThat(jwt.verify(new ECDSAVerifier((ECPublicKey) keyPair.getPublic()))).isTrue();
		assertThat(jwt.getHeader().getKeyID()).isEqualTo("KEY_ID");
		assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("TEAM_ID");
		assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("com.dewby.Nearby");
		assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("https://appleid.apple.com");
		assertThat(jwt.getJWTClaimsSet().getExpirationTime().toInstant())
				.isEqualTo(Instant.parse("2026-10-06T00:05:00Z"));
	}
}
