// 프로필 이미지 업로드 URL 발급 유스케이스 포트
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.IssueProfileImageUploadUrlCommand;
import com.sopt.nearby.companion.application.ProfileImageUploadUrlResult;

public interface IssueProfileImageUploadUrlUseCase {

	ProfileImageUploadUrlResult issue(IssueProfileImageUploadUrlCommand command);
}

