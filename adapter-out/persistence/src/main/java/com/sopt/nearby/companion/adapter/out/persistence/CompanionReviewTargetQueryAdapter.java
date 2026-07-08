// 동행 후기 대상 목록 쿼리 결과를 애플리케이션 조회 모델로 변환하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewTargetProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewTargetQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.review.CompanionReviewTarget;
import com.sopt.nearby.companion.port.out.CompanionReviewTargetQueryPort;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionReviewTargetQueryAdapter implements CompanionReviewTargetQueryPort {

	private final CompanionReviewTargetQueryJpaRepository repository;

	public CompanionReviewTargetQueryAdapter(final CompanionReviewTargetQueryJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<CompanionReviewTarget> findAllByMeetingIdAndReviewerUserIdAndTargetRole(
			final Long meetingId,
			final Long reviewerUserId,
			final MatchParticipantRole targetRole
	) {
		return repository.findAllByMeetingIdAndReviewerUserIdAndTargetRole(
						meetingId,
						reviewerUserId,
						targetRole.name()
				)
				.stream()
				.map(this::toTarget)
				.toList();
	}

	private CompanionReviewTarget toTarget(final CompanionReviewTargetProjection row) {
		return new CompanionReviewTarget(
				row.getRevieweeUserId(),
				row.getProfileImageUrl(),
				row.getNickname(),
				cityName(row.getPlaceAddress()),
				row.getMeetingAt().toLocalDate(),
				Boolean.TRUE.equals(row.getCheckedIn()),
				Boolean.TRUE.equals(row.getHasWrittenReview())
		);
	}

	private String cityName(final String placeAddress) {
		if (placeAddress == null || placeAddress.isBlank()) {
			return "";
		}
		String normalized = placeAddress.trim().replace(",", " ");
		int firstBlankIndex = normalized.indexOf(' ');
		if (firstBlankIndex < 0) {
			return normalized;
		}
		return normalized.substring(0, firstBlankIndex);
	}
}
