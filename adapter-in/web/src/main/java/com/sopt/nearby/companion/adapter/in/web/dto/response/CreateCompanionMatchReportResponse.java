// 매칭 기준의 동행 신고 등록 결과를 전달하는 응답 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.CreateCompanionReportResult;
import java.time.LocalDateTime;

public record CreateCompanionMatchReportResponse(
        Long matchId,
        Long reportId,
        LocalDateTime reportedAt
) {
    public static CreateCompanionMatchReportResponse from(final CreateCompanionReportResult result) {
        return new CreateCompanionMatchReportResponse(result.matchId(), result.reportId(), result.reportedAt());
    }
}
