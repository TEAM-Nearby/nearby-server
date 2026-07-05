// 프로필 이미지 업로드 URL 발급 유스케이스를 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.port.out.ProfileImageUploadRequest;
import com.sopt.nearby.companion.port.out.ProfileImageUploadUrl;
import com.sopt.nearby.companion.port.out.ProfileImageUploadUrlIssuer;
import java.util.Map;
import org.junit.jupiter.api.Test;

class IssueProfileImageUploadUrlServiceTest {

	private final FakeProfileImageUploadUrlIssuer issuer = new FakeProfileImageUploadUrlIssuer();
	private final IssueProfileImageUploadUrlService service = new IssueProfileImageUploadUrlService(
			issuer,
			5L * 1024 * 1024
	);

	@Test
	void issuesProfileImageUploadUrl() {
		issuer.result = new ProfileImageUploadUrl(
				"https://s3.example/upload",
				"https://cdn.nearby.com/profiles/1/image.jpg",
				"PUT",
				300,
				Map.of("Content-Type", "image/jpeg")
		);

		ProfileImageUploadUrlResult result = service.issue(new IssueProfileImageUploadUrlCommand(
				1L,
				"profile.jpg",
				"image/jpeg",
				524_288L
		));

		assertEquals(1L, issuer.request.userId());
		assertEquals("profile.jpg", issuer.request.fileName());
		assertEquals("image/jpeg", issuer.request.contentType());
		assertEquals(524_288L, issuer.request.fileSize());
		assertEquals("https://s3.example/upload", result.uploadUrl());
		assertEquals("https://cdn.nearby.com/profiles/1/image.jpg", result.imageUrl());
		assertEquals("PUT", result.method());
		assertEquals(300, result.expiresIn());
		assertEquals("image/jpeg", result.headers().get("Content-Type"));
	}

	@Test
	void rejectsUnsupportedContentType() {
		assertThrows(InvalidProfileImageUploadRequestException.class, () -> service.issue(
				new IssueProfileImageUploadUrlCommand(1L, "profile.gif", "image/gif", 524_288L)
		));
	}

	@Test
	void rejectsTooLargeFile() {
		assertThrows(InvalidProfileImageUploadRequestException.class, () -> service.issue(
				new IssueProfileImageUploadUrlCommand(1L, "profile.jpg", "image/jpeg", 5L * 1024 * 1024 + 1)
		));
	}

	static class FakeProfileImageUploadUrlIssuer implements ProfileImageUploadUrlIssuer {

		private ProfileImageUploadRequest request;
		private ProfileImageUploadUrl result;

		@Override
		public ProfileImageUploadUrl issue(final ProfileImageUploadRequest request) {
			this.request = request;
			return result;
		}
	}
}

