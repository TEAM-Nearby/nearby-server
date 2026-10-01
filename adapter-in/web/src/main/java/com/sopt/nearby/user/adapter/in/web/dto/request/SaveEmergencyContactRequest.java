// 비상 연락망 한 명의 저장 요청을 검증한다.
package com.sopt.nearby.user.adapter.in.web.dto.request;

import com.sopt.nearby.user.application.SaveEmergencyContactCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveEmergencyContactRequest(
        @Schema(description = "비상 연락망 이름", example = "홍길동")
        @NotBlank @Size(max = 50) String name,
        @Schema(description = "구분자 없는 8~15자리 전화번호. 국제번호는 +로 시작할 수 있습니다.", example = "01012345678")
        @NotBlank @Pattern(regexp = "\\+?[0-9]{8,15}") String phoneNumber
) {
    public SaveEmergencyContactCommand toCommand(final Long userId) {
        return new SaveEmergencyContactCommand(userId, name, phoneNumber);
    }
}
