// HTTP 응답 본문 수신까지 전체 제한 시간을 적용하고 초과 요청을 취소하는 유틸리티
package com.sopt.nearby.user.adapter.out.security;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class TimedHttpClient {

	private TimedHttpClient() {
	}

	static HttpResponse<String> send(
			final HttpClient httpClient,
			final HttpRequest request,
			final Duration timeout
	) throws Exception {
		CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(
				request,
				HttpResponse.BodyHandlers.ofString()
		);
		try {
			return response.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException | InterruptedException exception) {
			response.cancel(true);
			throw exception;
		}
	}
}
