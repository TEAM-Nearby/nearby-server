// 저장된 비상 연락망 정보를 API 응답으로 표현한다.
package com.sopt.nearby.user.adapter.in.web.dto.response;

import com.sopt.nearby.user.domain.model.EmergencyContact;
import io.swagger.v3.oas.annotations.media.Schema;

public record EmergencyContactResponse(
        @Schema(description = "비상 연락망 ID") Long contactId,
        @Schema(description = "이름") String name,
        @Schema(description = "전화번호") String phoneNumber
) {
    public static EmergencyContactResponse from(final EmergencyContact contact) {
        return new EmergencyContactResponse(contact.id(), contact.name(), contact.phoneNumber());
    }
}
