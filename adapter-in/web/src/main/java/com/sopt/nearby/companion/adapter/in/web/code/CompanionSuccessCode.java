// 동행 매칭 성공 코드 정의
package com.sopt.nearby.companion.adapter.in.web.code;

import com.sopt.nearby.shared.adapter.in.web.response.SuccessCode;

public enum CompanionSuccessCode implements SuccessCode {
    READ_COMPANION_MATCH_PREVIEW("매칭된 동행 미리보기 정보를 조회했어요."),
    READ_COMPANION_MATCHES("매칭된 동행 목록을 조회했어요."),
    CONFIRM_COMPANION_SCHEDULE("동행 일정이 확정되었어요."),
    PROFILE_IMAGE_UPLOAD_URL_ISSUED("Presigned URL 발급에 성공했습니다."),
    COMPANION_PROFILE_CREATED("동행 프로필 등록이 완료되었습니다.");

    private final String message;

    CompanionSuccessCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
