// 카카오 오픈채팅 URL 형식이 올바르지 않을 때 발생하는 예외다.
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidOpenChatUrlException extends BusinessException {

    public InvalidOpenChatUrlException() {
        super(CompanionErrorCode.INVALID_OPEN_CHAT_URL);
    }
}
