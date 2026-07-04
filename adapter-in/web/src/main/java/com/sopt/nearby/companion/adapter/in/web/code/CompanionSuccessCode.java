// 동행 매칭 성공 코드 정의
package com.sopt.nearby.companion.adapter.in.web.code;

import com.sopt.nearby.shared.adapter.in.web.response.SuccessCode;

public enum CompanionSuccessCode implements SuccessCode {
    READ_COMPANION_MATCH_PREVIEW("매칭된 동행 미리보기 정보를 조회했어요."),
    READ_COMPANION_MATCHES("매칭된 동행 목록을 조회했어요.");

    private final String message;

    CompanionSuccessCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
