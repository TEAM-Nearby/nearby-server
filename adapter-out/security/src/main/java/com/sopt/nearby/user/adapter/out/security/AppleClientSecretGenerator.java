// Apple OAuth 요청에 사용할 client_secret JWT를 생성하는 구성 요소
package com.sopt.nearby.user.adapter.out.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class AppleClientSecretGenerator {

	private static final String APPLE_ISSUER = "https://appleid.apple.com";
	private static final long CLIENT_SECRET_VALID_MINUTES = 5L;

	private final Clock clock;

	public AppleClientSecretGenerator(final Clock clock) {
		this.clock = clock;
	}

	public String generate(
			final String teamId,
			final String keyId,
			final String clientId,
			final String privateKeyBase64
	) {
		try {
			Instant now = clock.instant();
			SignedJWT jwt = new SignedJWT(
					new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(keyId).build(),
					new JWTClaimsSet.Builder()
							.issuer(teamId)
							.subject(clientId)
							.audience(APPLE_ISSUER)
							.issueTime(Date.from(now))
							.expirationTime(Date.from(now.plus(CLIENT_SECRET_VALID_MINUTES, ChronoUnit.MINUTES)))
							.build()
			);
			jwt.sign(new ECDSASigner(readPrivateKey(privateKeyBase64)));
			return jwt.serialize();
		} catch (Exception exception) {
			throw new IllegalStateException("Apple client_secret 생성에 실패했습니다.", exception);
		}
	}

	private ECPrivateKey readPrivateKey(final String privateKeyBase64) throws Exception {
		String pem = new String(Base64.getDecoder().decode(privateKeyBase64), StandardCharsets.UTF_8);
		String encodedKey = pem
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s", "");
		byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
		return (ECPrivateKey) KeyFactory.getInstance("EC")
				.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
	}
}
