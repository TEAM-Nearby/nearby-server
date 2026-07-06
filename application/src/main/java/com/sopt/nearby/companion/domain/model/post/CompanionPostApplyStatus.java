// 동행 모집 글 상세 조회에서 현재 사용자의 신청 상태를 표현한다.
package com.sopt.nearby.companion.domain.model.post;

public enum CompanionPostApplyStatus {
    NOT_APPLIED,
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELED
}
