// 프로필 이미지 업로드 URL 발급을 외부 스토리지에 위임하는 포트
package com.sopt.nearby.companion.port.out;

public interface ProfileImageUploadUrlIssuer {

	ProfileImageUploadUrl issue(ProfileImageUploadRequest request);
}

