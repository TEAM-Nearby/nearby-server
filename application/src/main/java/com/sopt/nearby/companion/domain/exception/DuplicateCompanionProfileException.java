// 한 사용자의 동행 프로필 중복 등록을 막는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class DuplicateCompanionProfileException extends ConflictException {

	public DuplicateCompanionProfileException() {
		super(CompanionErrorCode.DUPLICATE_COMPANION_PROFILE);
	}
}
