// JWT 액세스 토큰과 리프레시 토큰을 발급하는 어댑터
package com.sopt.nearby.user.adapter.out.security;

import com.sopt.nearby.user.application.IssuedTokens;
import com.sopt.nearby.user.application.TokenIssueRequest;
import com.sopt.nearby.user.port.out.TokenIssuer;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenAdapter implements TokenIssuer {

	private static final String ISSUER = "nearby";
	private static final int REFRESH_TOKEN_BYTE_LENGTH = 32;

	private final JwtEncoder jwtEncoder;
	private final long accessTokenTtlSeconds;
	private final long refreshTokenTtlSeconds;
	private final Clock clock;
	private final SecureRandom secureRandom = new SecureRandom();

	@Autowired
	public JwtTokenAdapter(
			final JwtEncoder jwtEncoder,
			@Value("${nearby.jwt.access-token-ttl-seconds:${NEARBY_ACCESS_TOKEN_TTL_SECONDS:3600}}")
			final long accessTokenTtlSeconds,
			@Value("${nearby.jwt.refresh-token-ttl-seconds:${NEARBY_REFRESH_TOKEN_TTL_SECONDS:1209600}}")
			final long refreshTokenTtlSeconds
	) {
		this(jwtEncoder, accessTokenTtlSeconds, refreshTokenTtlSeconds, Clock.systemUTC());
	}

	JwtTokenAdapter(
			final JwtEncoder jwtEncoder,
			final long accessTokenTtlSeconds,
			final long refreshTokenTtlSeconds,
			final Clock clock
	) {
		this.jwtEncoder = jwtEncoder;
		this.accessTokenTtlSeconds = accessTokenTtlSeconds;
		this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
		this.clock = clock;
	}

	@Override
	public IssuedTokens issue(final TokenIssueRequest request) {
		Instant issuedAt = clock.instant();
		String accessToken = issueAccessToken(request, issuedAt);
		String refreshToken = randomRefreshToken();

		return new IssuedTokens(
				accessToken,
				refreshToken,
				RefreshTokenHashSupport.sha256(refreshToken),
				accessTokenTtlSeconds,
				refreshTokenTtlSeconds
		);
	}

	private String issueAccessToken(final TokenIssueRequest request, final Instant issuedAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(String.valueOf(request.userId()))
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plusSeconds(accessTokenTtlSeconds))
				.claim("role", request.role().name())
				.claim("onboardingStatus", request.onboardingStatus().name())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	private String randomRefreshToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
