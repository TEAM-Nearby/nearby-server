// 프로필 이미지 업로드 URL 발급 요청의 형식 오류를 표현하는 예외
package com.sopt.nearby.companion.application;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidProfileImageUploadRequestException extends BusinessException {

	public InvalidProfileImageUploadRequestException() {
		super(CompanionErrorCode.INVALID_PROFILE_IMAGE_UPLOAD_REQUEST);
	}
}

