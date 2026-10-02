// 동행 신고 등록 유스케이스를 정의하는 입력 포트
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.CreateCompanionReportCommand;
import com.sopt.nearby.companion.application.CreateCompanionReportResult;

public interface CreateCompanionReportUseCase {

	CreateCompanionReportResult create(CreateCompanionReportCommand command);
}
