// 동행 신청 생성 UseCase를 정의하는 인터페이스
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.CreateCompanionRequestCommand;
import com.sopt.nearby.companion.application.CreateCompanionRequestResult;

public interface CreateCompanionRequestUseCase {

    CreateCompanionRequestResult create(CreateCompanionRequestCommand command);
}
