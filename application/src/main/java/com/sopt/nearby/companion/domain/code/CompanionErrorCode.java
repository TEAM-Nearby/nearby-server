package com.sopt.nearby.companion.domain.code;

import com.sopt.nearby.common.exception.ErrorCode;

public enum CompanionErrorCode implements ErrorCode {

    COMPANION_MATCH_NOT_FOUND("매칭 정보를 찾을 수 없습니다."),
    FORBIDDEN_COMPANION_MATCH("해당 매칭 정보를 조회할 권한이 없습니다."),
    COMPANION_MATCH_POST_NOT_FOUND("매칭 게시글을 찾을 수 없습니다."),
    INVALID_MATCH_ID("올바르지 않은 매칭 ID입니다."),
    COMPANION_PROFILE_NOT_FOUND("동행 프로필을 찾을 수 없습니다."),
    INVALID_SCHEDULE_REQUEST("올바르지 않은 일정 확정 요청입니다."),
    FORBIDDEN_COMPANION_SCHEDULE("동행 일정을 확정할 권한이 없습니다."),
    COMPANION_SCHEDULE_ALREADY_CONFIRMED("이미 확정된 동행 일정입니다."),
    COMPANION_MATCH_ALREADY_CANCELED("취소된 매칭의 일정은 확정할 수 없습니다."),
    COMPANION_MATCH_ALREADY_COMPLETED("완료된 매칭의 일정은 확정할 수 없습니다.");


    private final String message;

    CompanionErrorCode(final String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}