// S3 호환 스토리지의 프로필 이미지 업로드 URL을 발급하는 어댑터
package com.sopt.nearby.companion.adapter.out.storage;

import com.sopt.nearby.companion.port.out.ProfileImageUploadRequest;
import com.sopt.nearby.companion.port.out.ProfileImageUploadUrl;
import com.sopt.nearby.companion.port.out.ProfileImageUploadUrlIssuer;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Component
public class S3ProfileImageUploadUrlIssuer implements ProfileImageUploadUrlIssuer, DisposableBean {

	private static final String METHOD = "PUT";

	private final String bucket;
	private final String cdnBaseUrl;
	private final int expiresIn;
	private final S3Presigner presigner;
	private final Supplier<UUID> uuidSupplier;

	@Autowired
	public S3ProfileImageUploadUrlIssuer(
			@Value("${nearby.storage.s3.bucket:nearby}") final String bucket,
			@Value("${nearby.storage.s3.region:ap-northeast-2}") final String region,
			@Value("${nearby.storage.s3.cdn-base-url:https://cdn.nearby.com}") final String cdnBaseUrl,
			@Value("${nearby.storage.s3.upload-url-expires-seconds:300}") final int expiresIn
	) {
		this(
				bucket,
				cdnBaseUrl,
				expiresIn,
				S3Presigner.builder()
						.region(Region.of(region))
						.build(),
				UUID::randomUUID
		);
	}

	S3ProfileImageUploadUrlIssuer(
			final String bucket,
			final String cdnBaseUrl,
			final int expiresIn,
			final S3Presigner presigner,
			final Supplier<UUID> uuidSupplier
	) {
		this.bucket = bucket;
		this.cdnBaseUrl = trimTrailingSlash(cdnBaseUrl);
		this.expiresIn = expiresIn;
		this.presigner = presigner;
		this.uuidSupplier = uuidSupplier;
	}

	@Override
	public ProfileImageUploadUrl issue(final ProfileImageUploadRequest request) {
		String objectKey = objectKey(request.userId(), request.contentType());
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucket)
				.key(objectKey)
				.contentType(request.contentType())
				.build();
		PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
				PutObjectPresignRequest.builder()
						.signatureDuration(Duration.ofSeconds(expiresIn))
						.putObjectRequest(putObjectRequest)
						.build()
		);

		return new ProfileImageUploadUrl(
				presignedRequest.url().toString(),
				cdnBaseUrl + "/" + objectKey,
				METHOD,
				expiresIn,
				Map.of("Content-Type", request.contentType())
		);
	}

	private String objectKey(final Long userId, final String contentType) {
		return "profiles/" + userId + "/" + uuidSupplier.get() + extension(contentType);
	}

	private static String extension(final String contentType) {
		return switch (contentType) {
			case "image/png" -> ".png";
			case "image/webp" -> ".webp";
			default -> ".jpg";
		};
	}

	private static String trimTrailingSlash(final String value) {
		if (value.endsWith("/")) {
			return value.substring(0, value.length() - 1);
		}
		return value;
	}

	@Override
	public void destroy() {
		presigner.close();
	}
}
