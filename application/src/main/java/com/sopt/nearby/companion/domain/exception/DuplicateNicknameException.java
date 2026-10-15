// 중복된 동행 프로필 닉네임 등록을 막는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class DuplicateNicknameException extends ConflictException {

	public DuplicateNicknameException() {
		super(CompanionErrorCode.DUPLICATE_NICKNAME);
	}
}
