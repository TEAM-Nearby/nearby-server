// 동행 후기 등록 유스케이스의 진입 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.CreateCompanionReviewsCommand;
import com.sopt.nearby.companion.application.CreateCompanionReviewsResult;

public interface CreateCompanionReviewsUseCase {

	CreateCompanionReviewsResult create(CreateCompanionReviewsCommand command);
}
