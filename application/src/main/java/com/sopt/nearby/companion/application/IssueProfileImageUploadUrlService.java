// 프로필 이미지 업로드 URL 발급 규칙을 처리하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.port.in.IssueProfileImageUploadUrlUseCase;
import com.sopt.nearby.companion.port.out.ProfileImageUploadRequest;
import com.sopt.nearby.companion.port.out.ProfileImageUploadUrl;
import com.sopt.nearby.companion.port.out.ProfileImageUploadUrlIssuer;
import java.util.Set;

public class IssueProfileImageUploadUrlService implements IssueProfileImageUploadUrlUseCase {

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	private final ProfileImageUploadUrlIssuer issuer;
	private final long maxFileSizeBytes;

	public IssueProfileImageUploadUrlService(
			final ProfileImageUploadUrlIssuer issuer,
			final long maxFileSizeBytes
	) {
		this.issuer = issuer;
		this.maxFileSizeBytes = maxFileSizeBytes;
	}

	@Override
	public ProfileImageUploadUrlResult issue(final IssueProfileImageUploadUrlCommand command) {
		if (!ALLOWED_CONTENT_TYPES.contains(command.contentType())
				|| command.fileSize() <= 0
				|| command.fileSize() > maxFileSizeBytes) {
			throw new InvalidProfileImageUploadRequestException();
		}

		ProfileImageUploadUrl issued = issuer.issue(new ProfileImageUploadRequest(
				command.userId(),
				command.fileName(),
				command.contentType(),
				command.fileSize()
		));
		return new ProfileImageUploadUrlResult(
				issued.uploadUrl(),
				issued.imageUrl(),
				issued.method(),
				issued.expiresIn(),
				issued.headers()
		);
	}
}

