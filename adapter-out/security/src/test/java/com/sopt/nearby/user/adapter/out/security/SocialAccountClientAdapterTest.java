// Apple과 카카오 계정 연동 API 요청 형식을 검증하는 테스트
package com.sopt.nearby.user.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SocialAccountClientAdapterTest {

	private HttpServer server;

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void exchangesAppleAuthorizationCodeForRefreshToken() throws Exception {
		AtomicReference<String> requestBody = new AtomicReference<>();
		URI uri = startServer(exchange -> {
			requestBody.set(readBody(exchange));
			respond(exchange, 200, "{\"refresh_token\":\"apple-refresh-token\",\"id_token\":\"apple-id-token\"}");
		});
		AppleOAuthClientAdapter adapter = appleAdapter(uri, uri);

		var tokens = adapter.exchangeAuthorizationCode("authorization-code");

		assertThat(tokens.refreshToken()).isEqualTo("apple-refresh-token");
		assertThat(tokens.idToken()).isEqualTo("apple-id-token");
		assertThat(requestBody.get())
				.contains("client_id=com.dewby.Nearby")
				.contains("client_secret=generated-client-secret")
				.contains("code=authorization-code")
				.contains("grant_type=authorization_code");
	}

	@Test
	void revokesAppleRefreshToken() throws Exception {
		AtomicReference<String> requestBody = new AtomicReference<>();
		URI uri = startServer(exchange -> {
			requestBody.set(readBody(exchange));
			respond(exchange, 200, "");
		});

		appleAdapter(uri, uri).revoke("apple-refresh-token");

		assertThat(requestBody.get())
				.contains("token=apple-refresh-token")
				.contains("token_type_hint=refresh_token");
	}

	@Test
	void unlinksKakaoUserWithAdminKey() throws Exception {
		AtomicReference<String> authorization = new AtomicReference<>();
		AtomicReference<String> requestBody = new AtomicReference<>();
		URI uri = startServer(exchange -> {
			authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
			requestBody.set(readBody(exchange));
			respond(exchange, 200, "{\"id\":1234}");
		});
		KakaoAccountUnlinkerAdapter adapter = new KakaoAccountUnlinkerAdapter(
				HttpClient.newHttpClient(), "admin-key", uri
		);

		adapter.unlink("1234");

		assertThat(authorization.get()).isEqualTo("KakaoAK admin-key");
		assertThat(requestBody.get()).isEqualTo("target_id_type=user_id&target_id=1234");
	}

	@Test
	void rejectsMissingKakaoAdminKeyAtConstruction() {
		assertThrows(IllegalStateException.class,
				() -> new KakaoAccountUnlinkerAdapter(HttpClient.newHttpClient(), " ", URI.create("https://example.com")));
	}

	private AppleOAuthClientAdapter appleAdapter(final URI tokenUri, final URI revokeUri) {
		AppleClientSecretGenerator generator = new AppleClientSecretGenerator(Clock.systemUTC()) {
			@Override
			public String generate(
					final String teamId,
					final String keyId,
					final String clientId,
					final String privateKeyBase64
			) {
				return "generated-client-secret";
			}
		};
		return new AppleOAuthClientAdapter(
				HttpClient.newHttpClient(),
				new ObjectMapper(),
				generator,
				"TEAM_ID",
				"KEY_ID",
				"com.dewby.Nearby",
				"private-key",
				tokenUri,
				revokeUri
		);
	}

	private URI startServer(final ThrowingHandler handler) throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/oauth", exchange -> {
			try {
				handler.handle(exchange);
			} catch (Exception exception) {
				respond(exchange, 500, "");
			}
		});
		server.start();
		return URI.create("http://localhost:" + server.getAddress().getPort() + "/oauth");
	}

	private String readBody(final HttpExchange exchange) throws IOException {
		return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
	}

	private void respond(final HttpExchange exchange, final int status, final String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(status, bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}

	@FunctionalInterface
	private interface ThrowingHandler {
		void handle(HttpExchange exchange) throws Exception;
	}
}
