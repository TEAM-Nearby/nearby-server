// 동행 후기 등록 유스케이스 입력값을 표현하는 명령 객체
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import java.util.List;

public record CreateCompanionReviewsCommand(
		Long reviewerUserId,
		Long meetingId,
		Long revieweeUserId,
		int rating,
		List<ReviewKeyword> keywords
) {
}
