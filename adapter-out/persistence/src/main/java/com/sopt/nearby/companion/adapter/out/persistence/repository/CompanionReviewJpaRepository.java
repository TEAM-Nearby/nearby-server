// 동행 리뷰 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionReviewJpaRepository extends JpaRepository<CompanionReviewEntity, Long> {

	boolean existsByMeetingIdAndReviewerUserIdAndRevieweeUserId(
			Long meetingId,
			Long reviewerUserId,
			Long revieweeUserId
	);
}
