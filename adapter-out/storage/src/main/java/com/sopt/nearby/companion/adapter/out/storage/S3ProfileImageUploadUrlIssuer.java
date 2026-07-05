// S3 호환 스토리지의 프로필 이미지 업로드 URL을 발급하는 어댑터
package com.sopt.nearby.companion.adapter.out.storage;

import com.sopt.nearby.companion.port.out.ProfileImageUploadRequest;
import com.sopt.nearby.companion.port.out.ProfileImageUploadUrl;
import com.sopt.nearby.companion.port.out.ProfileImageUploadUrlIssuer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Supplier;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class S3ProfileImageUploadUrlIssuer implements ProfileImageUploadUrlIssuer {

	private static final String METHOD = "PUT";
	private static final String SERVICE = "s3";
	private static final String ALGORITHM = "AWS4-HMAC-SHA256";
	private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
	private static final DateTimeFormatter AMZ_DATE_FORMATTER = DateTimeFormatter
			.ofPattern("yyyyMMdd'T'HHmmss'Z'")
			.withZone(ZoneOffset.UTC);
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
			.ofPattern("yyyyMMdd")
			.withZone(ZoneOffset.UTC);

	private final String bucket;
	private final String region;
	private final String accessKey;
	private final String secretKey;
	private final String cdnBaseUrl;
	private final int expiresIn;
	private final Clock clock;
	private final Supplier<UUID> uuidSupplier;

	@Autowired
	public S3ProfileImageUploadUrlIssuer(
			@Value("${nearby.storage.s3.bucket:nearby}") final String bucket,
			@Value("${nearby.storage.s3.region:ap-northeast-2}") final String region,
			@Value("${nearby.storage.s3.access-key:test-access-key}") final String accessKey,
			@Value("${nearby.storage.s3.secret-key:test-secret-key}") final String secretKey,
			@Value("${nearby.storage.s3.cdn-base-url:https://cdn.nearby.com}") final String cdnBaseUrl,
			@Value("${nearby.storage.s3.upload-url-expires-seconds:300}") final int expiresIn
	) {
		this(bucket, region, accessKey, secretKey, cdnBaseUrl, expiresIn, Clock.systemUTC(), UUID::randomUUID);
	}

	S3ProfileImageUploadUrlIssuer(
			final String bucket,
			final String region,
			final String accessKey,
			final String secretKey,
			final String cdnBaseUrl,
			final int expiresIn,
			final Clock clock,
			final Supplier<UUID> uuidSupplier
	) {
		this.bucket = bucket;
		this.region = region;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.cdnBaseUrl = trimTrailingSlash(cdnBaseUrl);
		this.expiresIn = expiresIn;
		this.clock = clock;
		this.uuidSupplier = uuidSupplier;
	}

	@Override
	public ProfileImageUploadUrl issue(final ProfileImageUploadRequest request) {
		String objectKey = objectKey(request.userId(), request.contentType());
		String host = bucket + ".s3." + region + ".amazonaws.com";
		Instant now = clock.instant();
		String date = DATE_FORMATTER.format(now);
		String amzDate = AMZ_DATE_FORMATTER.format(now);
		String credentialScope = date + "/" + region + "/" + SERVICE + "/aws4_request";
		String signedHeaders = "content-type;host";

		TreeMap<String, String> query = new TreeMap<>();
		query.put("X-Amz-Algorithm", ALGORITHM);
		query.put("X-Amz-Credential", accessKey + "/" + credentialScope);
		query.put("X-Amz-Date", amzDate);
		query.put("X-Amz-Expires", String.valueOf(expiresIn));
		query.put("X-Amz-SignedHeaders", signedHeaders);

		String canonicalUri = "/" + encodePath(objectKey);
		String canonicalQuery = canonicalQuery(query);
		String canonicalHeaders = "content-type:" + request.contentType() + "\n" + "host:" + host + "\n";
		String canonicalRequest = METHOD + "\n"
				+ canonicalUri + "\n"
				+ canonicalQuery + "\n"
				+ canonicalHeaders + "\n"
				+ signedHeaders + "\n"
				+ UNSIGNED_PAYLOAD;
		String stringToSign = ALGORITHM + "\n"
				+ amzDate + "\n"
				+ credentialScope + "\n"
				+ sha256Hex(canonicalRequest);
		String signature = hmacHex(signingKey(date), stringToSign);

		return new ProfileImageUploadUrl(
				"https://" + host + canonicalUri + "?" + canonicalQuery + "&X-Amz-Signature=" + signature,
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

	private static String canonicalQuery(final TreeMap<String, String> query) {
		return query.entrySet()
				.stream()
				.map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
				.reduce((left, right) -> left + "&" + right)
				.orElse("");
	}

	private static String encodePath(final String value) {
		String[] parts = value.split("/");
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (i > 0) {
				builder.append('/');
			}
			builder.append(encode(parts[i]));
		}
		return builder.toString();
	}

	private static String encode(final String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8)
				.replace("+", "%20")
				.replace("%7E", "~");
	}

	private static String sha256Hex(final String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
		}
	}

	private byte[] signingKey(final String date) {
		byte[] dateKey = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), date);
		byte[] dateRegionKey = hmac(dateKey, region);
		byte[] dateRegionServiceKey = hmac(dateRegionKey, SERVICE);
		return hmac(dateRegionServiceKey, "aws4_request");
	}

	private static String hmacHex(final byte[] key, final String value) {
		return HexFormat.of().formatHex(hmac(key, value));
	}

	private static byte[] hmac(final byte[] key, final String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException | InvalidKeyException exception) {
			throw new IllegalStateException("HMAC-SHA256 서명을 생성할 수 없습니다.", exception);
		}
	}

	private static String trimTrailingSlash(final String value) {
		if (value.endsWith("/")) {
			return value.substring(0, value.length() - 1);
		}
		return value;
	}
}
