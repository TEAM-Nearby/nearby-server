// 동행 신청 거절 요청 본문을 표현한다.
package com.sopt.nearby.companion.adapter.in.web.dto.request;

public record RejectCompanionRequestRequest(
        String rejectionReason
) {
}
