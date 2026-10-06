// 카카오 Admin Key로 사용자 계정 연동 해제 API를 호출하는 어댑터
package com.sopt.nearby.user.adapter.out.security;

import com.sopt.nearby.user.exception.SocialAccountUnlinkFailedException;
import com.sopt.nearby.user.port.out.KakaoAccountUnlinker;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KakaoAccountUnlinkerAdapter implements KakaoAccountUnlinker {

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

	private final HttpClient httpClient;
	private final String adminKey;
	private final URI unlinkUri;

	@Autowired
	public KakaoAccountUnlinkerAdapter(
			@Value("${kakao.admin-key:}") final String adminKey,
			@Value("${kakao.unlink-uri:https://kapi.kakao.com/v1/user/unlink}") final URI unlinkUri
	) {
		this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build(), requireAdminKey(adminKey), unlinkUri);
	}

	KakaoAccountUnlinkerAdapter(final HttpClient httpClient, final String adminKey, final URI unlinkUri) {
		this.httpClient = httpClient;
		this.adminKey = requireAdminKey(adminKey);
		this.unlinkUri = unlinkUri;
	}

	@Override
	public void unlink(final String providerUserId) {
		if (isBlank(adminKey) || isBlank(providerUserId)) {
			throw new SocialAccountUnlinkFailedException();
		}
		String body = "target_id_type=user_id&target_id="
				+ URLEncoder.encode(providerUserId, StandardCharsets.UTF_8);
		HttpRequest request = HttpRequest.newBuilder(unlinkUri)
				.timeout(REQUEST_TIMEOUT)
				.header("Authorization", "KakaoAK " + adminKey)
				.header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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

	private boolean isBlank(final String value) {
		return value == null || value.isBlank();
	}

	private static String requireAdminKey(final String adminKey) {
		if (adminKey == null || adminKey.isBlank()) {
			throw new IllegalStateException("kakao.admin-key 설정이 필요합니다.");
		}
		return adminKey;
	}
}
