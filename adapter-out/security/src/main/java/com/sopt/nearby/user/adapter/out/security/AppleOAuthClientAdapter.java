// Apple 인증 서버의 코드 교환과 토큰 폐기 API를 호출하는 어댑터
package com.sopt.nearby.user.adapter.out.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.user.exception.AppleLoginFailedException;
import com.sopt.nearby.user.exception.SocialAccountUnlinkFailedException;
import com.sopt.nearby.user.port.out.AppleOAuthClient;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppleOAuthClientAdapter implements AppleOAuthClient {

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
	private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final AppleClientSecretGenerator clientSecretGenerator;
	private final String teamId;
	private final String keyId;
	private final String clientId;
	private final String privateKeyBase64;
	private final URI tokenUri;
	private final URI revokeUri;

	@Autowired
	public AppleOAuthClientAdapter(
			final ObjectMapper objectMapper,
			final AppleClientSecretGenerator clientSecretGenerator,
			@Value("${apple.team-id:}") final String teamId,
			@Value("${apple.key-id:}") final String keyId,
			@Value("${apple.client-id:}") final String clientId,
			@Value("${apple.private-key-base64:}") final String privateKeyBase64,
			@Value("${apple.oauth.token-uri:https://appleid.apple.com/auth/token}") final URI tokenUri,
			@Value("${apple.oauth.revoke-uri:https://appleid.apple.com/auth/revoke}") final URI revokeUri
	) {
		this(
				HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(),
				objectMapper,
				clientSecretGenerator,
				teamId,
				keyId,
				clientId,
				privateKeyBase64,
				tokenUri,
				revokeUri
		);
	}

	AppleOAuthClientAdapter(
			final HttpClient httpClient,
			final ObjectMapper objectMapper,
			final AppleClientSecretGenerator clientSecretGenerator,
			final String teamId,
			final String keyId,
			final String clientId,
			final String privateKeyBase64,
			final URI tokenUri,
			final URI revokeUri
	) {
		this.httpClient = httpClient;
		this.objectMapper = objectMapper;
		this.clientSecretGenerator = clientSecretGenerator;
		this.teamId = teamId;
		this.keyId = keyId;
		this.clientId = clientId;
		this.privateKeyBase64 = privateKeyBase64;
		this.tokenUri = tokenUri;
		this.revokeUri = revokeUri;
	}

	@Override
	public Tokens exchangeAuthorizationCode(final String authorizationCode) {
		if (isBlank(authorizationCode)) {
			throw new AppleLoginFailedException();
		}
		try {
			HttpResponse<String> response = send(tokenUri, Map.of(
					"client_id", clientId,
					"client_secret", clientSecret(),
					"code", authorizationCode,
					"grant_type", "authorization_code"
			));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new AppleLoginFailedException();
			}
			JsonNode responseBody = objectMapper.readTree(response.body());
			String refreshToken = responseBody.path("refresh_token").asText();
			String idToken = responseBody.path("id_token").asText();
			if (isBlank(refreshToken) || isBlank(idToken)) {
				throw new AppleLoginFailedException();
			}
			return new Tokens(refreshToken, idToken);
		} catch (AppleLoginFailedException exception) {
			throw exception;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AppleLoginFailedException();
		} catch (Exception exception) {
			throw new AppleLoginFailedException();
		}
	}

	@Override
	public void revoke(final String refreshToken) {
		if (isBlank(refreshToken)) {
			throw new SocialAccountUnlinkFailedException();
		}
		try {
			HttpResponse<String> response = send(revokeUri, Map.of(
					"client_id", clientId,
					"client_secret", clientSecret(),
					"token", refreshToken,
					"token_type_hint", "refresh_token"
			));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new SocialAccountUnlinkFailedException();
			}
		} catch (SocialAccountUnlinkFailedException exception) {
			throw exception;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new SocialAccountUnlinkFailedException();
		} catch (Exception exception) {
			throw new SocialAccountUnlinkFailedException();
		}
	}

	private HttpResponse<String> send(final URI uri, final Map<String, String> fields) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(REQUEST_TIMEOUT)
				.header("Content-Type", FORM_CONTENT_TYPE)
				.POST(HttpRequest.BodyPublishers.ofString(form(fields)))
				.build();
		return TimedHttpClient.send(httpClient, request, REQUEST_TIMEOUT);
	}

	private String clientSecret() {
		if (isBlank(teamId) || isBlank(keyId) || isBlank(clientId) || isBlank(privateKeyBase64)) {
			throw new IllegalStateException("Apple OAuth 환경변수가 설정되지 않았습니다.");
		}
		return clientSecretGenerator.generate(teamId, keyId, clientId, privateKeyBase64);
	}

	private String form(final Map<String, String> fields) {
		return fields.entrySet().stream()
				.map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
				.collect(Collectors.joining("&"));
	}

	private String encode(final String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private boolean isBlank(final String value) {
		return value == null || value.isBlank();
	}
}
