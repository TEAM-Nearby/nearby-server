// S3 프로필 이미지 업로드 URL 발급 어댑터를 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.companion.port.out.ProfileImageUploadRequest;
import com.sopt.nearby.companion.port.out.ProfileImageUploadUrl;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class S3ProfileImageUploadUrlIssuerTest {

	@Test
	void createsIssuerWithoutResolvingCredentialsAtStartup() throws Exception {
		S3ProfileImageUploadUrlIssuer issuer = new S3ProfileImageUploadUrlIssuer(
				"nearby",
				"ap-northeast-2",
				"https://cdn.nearby.com",
				300
		);

		issuer.destroy();
	}

	@Test
	void issuesPresignedPutUrlAndCdnImageUrl() {
		try (S3Presigner presigner = sessionPresigner()) {
			S3ProfileImageUploadUrlIssuer issuer = new S3ProfileImageUploadUrlIssuer(
					"nearby",
					"https://cdn.nearby.com",
					300,
					presigner,
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
					.contains("X-Amz-Security-Token=test-session-token")
					.contains("X-Amz-Signature=");
		}
	}

	@Test
	void createsImageUrlExtensionFromContentType() {
		try (S3Presigner presigner = sessionPresigner()) {
			S3ProfileImageUploadUrlIssuer issuer = new S3ProfileImageUploadUrlIssuer(
					"nearby",
					"https://cdn.nearby.com/",
					300,
					presigner,
					() -> UUID.fromString("00000000-0000-0000-0000-000000000001")
			);

			assertThat(issue(issuer, "image/jpeg").imageUrl()).endsWith(".jpg");
			assertThat(issue(issuer, "image/png").imageUrl()).endsWith(".png");
			assertThat(issue(issuer, "image/webp").imageUrl()).endsWith(".webp");
		}
	}

	private static ProfileImageUploadUrl issue(
			final S3ProfileImageUploadUrlIssuer issuer,
			final String contentType
	) {
		return issuer.issue(new ProfileImageUploadRequest(7L, "profile", contentType, 524_288L));
	}

	private static S3Presigner sessionPresigner() {
		return S3Presigner.builder()
				.region(Region.AP_NORTHEAST_2)
				.credentialsProvider(StaticCredentialsProvider.create(AwsSessionCredentials.create(
						"test-access-key",
						"test-secret-key",
						"test-session-token"
				)))
				.build();
	}
}
