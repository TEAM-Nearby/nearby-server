// 동행 신청 생성 결과를 클라이언트에 반환하는 응답 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.CreateCompanionRequestResult;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import java.time.LocalDateTime;

public record CreatedCompanionRequestResponse(
        Long applicationId,
        Long postId,
        CompanionApplicationStatus applicationStatus,
        LocalDateTime createdAt
) {

    public static CreatedCompanionRequestResponse from(final CreateCompanionRequestResult result) {
        return new CreatedCompanionRequestResponse(
                result.applicationId(),
                result.postId(),
                result.applicationStatus(),
                result.createdAt()
        );
    }
}
