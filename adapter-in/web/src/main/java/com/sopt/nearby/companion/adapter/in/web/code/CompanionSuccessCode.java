// 동행 매칭 성공 코드 정의
package com.sopt.nearby.companion.adapter.in.web.code;

import com.sopt.nearby.shared.adapter.in.web.response.SuccessCode;

public enum CompanionSuccessCode implements SuccessCode {
    COMPANION_POST_CREATED("동행 모집 글 작성에 성공했습니다."),
    COMPANION_POSTS_FOUND("동행 모집 글 목록 조회에 성공했습니다."),
    COMPANION_POST_FOUND("동행 모집 글 상세 조회에 성공했어요."),
    READ_COMPANION_MATCH_PREVIEW("매칭된 동행 미리보기 정보를 조회했어요."),
    READ_COMPANION_MATCHES("매칭된 동행 목록을 조회했어요."),
    CONFIRM_COMPANION_SCHEDULE("동행 일정이 확정되었어요."),
    READ_COMPANION_SCHEDULE("동행 일정 정보를 조회했어요."),
    PROFILE_IMAGE_UPLOAD_URL_ISSUED("Presigned URL 발급에 성공했습니다."),
    COMPANION_PROFILE_CREATED("동행 프로필 등록이 완료되었습니다."),
    COMPANION_PROFILE_FOUND("동행 프로필 조회에 성공했어요."),
    READ_COMPANION_REQUESTS("동행 요청 목록을 조회했어요."),
    MARK_COMPANION_NOTIFICATION_AS_READ("동행 알림을 읽음 처리했어요."),
    READ_ONGOING_COMPANION_MEETINGS("현재 진행 중인 동행 목록을 조회했어요."),
    READ_COMPANION_MEETING_DETAIL("진행 중인 동행 상세 정보를 조회했어요."),
    CHECK_IN_COMPANION_MEETING("만남 인증이 완료되었어요."),
    CHECK_IN_COMPANION_MEETING_ALREADY_COMPLETED("이미 만남 인증이 완료되어 있어요.");

    private final String message;

    CompanionSuccessCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
