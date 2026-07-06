// S3 프로필 이미지 업로드 URL 발급 어댑터를 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.companion.port.out.ProfileImageUploadRequest;
import com.sopt.nearby.companion.port.out.ProfileImageUploadUrl;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.junit.jupiter.api.Test;

class S3ProfileImageUploadUrlIssuerTest {

	@Test
	void issuesPresignedPutUrlAndCdnImageUrl() {
		S3ProfileImageUploadUrlIssuer issuer = new S3ProfileImageUploadUrlIssuer(
				"nearby",
				"ap-northeast-2",
				"test-access-key",
				"test-secret-key",
				"https://cdn.nearby.com",
				300,
				Clock.fixed(Instant.parse("2026-07-05T07:00:00Z"), ZoneOffset.UTC),
				() -> UUID.fromString("00000000-0000-0000-0000-000000000001")
		);

		ProfileImageUploadUrl result = issuer.issue(new ProfileImageUploadRequest(
				7L,
				"profile.jpg",
				"image/jpeg",
				524_288L
		));

		assertThat(result.method()).isEqualTo("PUT");
		assertThat(result.expiresIn()).isEqualTo(300);
		assertThat(result.headers()).containsEntry("Content-Type", "image/jpeg");
		assertThat(result.imageUrl())
				.isEqualTo("https://cdn.nearby.com/profiles/7/00000000-0000-0000-0000-000000000001.jpg");
		assertThat(result.uploadUrl())
				.startsWith("https://nearby.s3.ap-northeast-2.amazonaws.com/profiles/7/")
				.contains("X-Amz-Algorithm=AWS4-HMAC-SHA256")
				.contains("X-Amz-Expires=300")
				.contains("X-Amz-SignedHeaders=content-type%3Bhost")
				.contains("X-Amz-Signature=");
	}

	@Test
	void allowsMissingAwsCredentialsAtStartup() {
		Constructor<?> constructor = S3ProfileImageUploadUrlIssuer.class.getConstructors()[0];

		assertThat(valueExpression(constructor.getParameters()[2]))
				.isEqualTo("${nearby.storage.s3.access-key:}");
		assertThat(valueExpression(constructor.getParameters()[3]))
				.isEqualTo("${nearby.storage.s3.secret-key:}");
	}

	@Test
	void rejectsIssueWhenAwsCredentialsAreMissing() {
		S3ProfileImageUploadUrlIssuer issuer = new S3ProfileImageUploadUrlIssuer(
				"nearby",
				"ap-northeast-2",
				"",
				"",
				"https://cdn.nearby.com",
				300,
				Clock.fixed(Instant.parse("2026-07-05T07:00:00Z"), ZoneOffset.UTC),
				() -> UUID.fromString("00000000-0000-0000-0000-000000000001")
		);

		assertThatThrownBy(() -> issuer.issue(new ProfileImageUploadRequest(
				7L,
				"profile.jpg",
				"image/jpeg",
				524_288L
		)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("S3 storage credentials are not configured.");
	}

	private static String valueExpression(final Parameter parameter) {
		return parameter.getAnnotation(Value.class).value();
	}
}
