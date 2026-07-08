package com.sopt.nearby.companion.domain.code;

import com.sopt.nearby.common.exception.ErrorCode;

public enum CompanionErrorCode implements ErrorCode {

    VALIDATION_ERROR("위도, 경도, 반경, 카테고리, 정렬 기준이 올바르지 않습니다."),
    COMPANION_POST_NOT_FOUND("동행 모집글을 찾을 수 없습니다."),
    COMPANION_POST_EXPIRED("마감된 글입니다."),
    COMPANION_REQUEST_ALREADY_EXISTS("이미 신청한 동행입니다."),
    COMPANION_POST_NOT_RECRUITING("모집 중인 동행이 아닙니다."),
    COMPANION_MATCH_NOT_FOUND("매칭 정보를 찾을 수 없습니다."),
    FORBIDDEN_COMPANION_MATCH("해당 매칭 정보를 조회할 권한이 없습니다."),
    COMPANION_MATCH_POST_NOT_FOUND("매칭 게시글을 찾을 수 없습니다."),
    INVALID_MATCH_ID("올바르지 않은 매칭 ID입니다."),
    COMPANION_PROFILE_NOT_FOUND("동행 프로필을 찾을 수 없습니다."),
    INVALID_SCHEDULE_REQUEST("올바르지 않은 일정 확정 요청입니다."),
    FORBIDDEN_COMPANION_SCHEDULE("동행 일정을 확정할 권한이 없습니다."),
    FORBIDDEN_READ_COMPANION_SCHEDULE("동행 일정을 조회할 권한이 없습니다."),
    COMPANION_SCHEDULE_ALREADY_CONFIRMED("이미 확정된 동행 일정입니다."),
    COMPANION_MATCH_ALREADY_CANCELED("취소된 매칭의 일정은 확정할 수 없습니다."),
    COMPANION_MATCH_ALREADY_COMPLETED("완료된 매칭의 일정은 확정할 수 없습니다."),
    COMPANION_MATCH_SCHEDULE_NOT_READABLE("취소된 매칭의 일정은 조회할 수 없습니다."),
    DUPLICATE_NICKNAME("이미 사용 중인 닉네임입니다."),
    DUPLICATE_COMPANION_PROFILE("이미 등록된 동행 프로필입니다."),
    INVALID_PROFILE_IMAGE_UPLOAD_REQUEST("지원하지 않는 이미지 형식이거나 또는 파일 크기를 초과했습니다."),
    INVALID_OPEN_CHAT_URL("카카오톡 오픈채팅 링크 형식이 올바르지 않습니다."),
    INVALID_REQUEST_DIRECTION("올바르지 않은 요청 방향입니다."),
    INVALID_NOTIFICATION_ID("올바르지 않은 알림 ID입니다."),
    FORBIDDEN_COMPANION_NOTIFICATION("동행 알림을 읽음 처리할 권한이 없습니다."),
    COMPANION_NOTIFICATION_NOT_FOUND("동행 알림을 찾을 수 없습니다."),
    INVALID_CHECK_IN_REQUEST("올바르지 않은 만남 인증 요청입니다."),
    OUT_OF_CHECK_IN_RADIUS("만남 인증 가능 반경 밖에 있습니다."),
    FORBIDDEN_COMPANION_MEETING("해당 동행의 참여자만 만남 인증을 할 수 있습니다."),
    COMPANION_MEETING_NOT_FOUND("동행 만남 정보를 찾을 수 없습니다."),
    COMPANION_MEETING_ALREADY_CANCELED("취소된 동행은 만남 인증을 할 수 없습니다."),
    COMPANION_MEETING_ALREADY_COMPLETED("완료된 동행은 만남 인증을 할 수 없습니다."),
    COMPANION_SCHEDULE_NOT_CONFIRMED("확정된 일정이 없어 만남 인증을 할 수 없습니다."),
    CHECK_IN_TIME_NOT_ALLOWED("만남 인증 가능 시간이 아닙니다."),
    INVALID_REVIEW_REQUEST("올바르지 않은 후기 등록 요청입니다."),
    INVALID_REVIEW_TARGET("올바르지 않은 후기 대상입니다."),
    INVALID_REVIEW_RATING("별점은 1점부터 5점까지의 정수여야 합니다."),
    INVALID_REVIEW_KEYWORD("올바르지 않은 후기 키워드입니다."),
    INVALID_REVIEW_KEYWORD_COUNT("배려 소통은 1개 이상 3개 이하, 시간 약속은 1개를 선택해야 합니다."),
    CANNOT_REVIEW_SELF("자기 자신에게 후기를 남길 수 없습니다."),
    FORBIDDEN_COMPANION_REVIEW("해당 동행의 참여자만 후기를 등록할 수 있습니다."),
    FORBIDDEN_COMPANION_REVIEW_TARGET("해당 동행의 참여자만 후기 대상 목록을 조회할 수 있습니다."),
    FORBIDDEN_COMPLETE_COMPANION_MEETING("해당 동행의 참여자만 동행을 마칠 수 있습니다."),
    COMPLETE_COMPANION_MEETING_ALREADY_CANCELED("취소된 동행은 완료할 수 없습니다."),
    COMPLETE_COMPANION_MEETING_ALREADY_COMPLETED("이미 완료된 동행입니다."),
    COMPLETE_COMPANION_MEETING_CURRENT_USER_NOT_CHECKED_IN("만남 인증을 완료한 후 동행을 마칠 수 있습니다."),
    REVIEWEE_NOT_FOUND("후기 대상 사용자를 찾을 수 없습니다."),
    COMPANION_REVIEW_MEETING_ALREADY_CANCELED("취소된 동행에는 후기를 남길 수 없습니다."),
    COMPANION_REVIEW_ALREADY_EXISTS("이미 해당 사용자에게 후기를 남겼습니다."),
    CURRENT_USER_NOT_CHECKED_IN("만남 인증을 완료한 후 후기를 남길 수 있습니다."),
    REVIEWEE_NOT_CHECKED_IN("만남 인증을 완료한 사용자에게만 후기를 남길 수 있습니다.");

    private final String message;

    CompanionErrorCode(final String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
