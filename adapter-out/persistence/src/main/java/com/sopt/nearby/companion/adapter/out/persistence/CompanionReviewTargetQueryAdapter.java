// 동행 후기 대상 목록 쿼리 결과를 애플리케이션 조회 모델로 변환하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewTargetProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewTargetQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.application.CompanionPlaceTimeResolver;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.domain.model.review.CompanionReviewTarget;
import com.sopt.nearby.companion.port.out.CompanionReviewTargetQueryPort;
import java.time.ZoneOffset;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;

@Repository
public class CompanionReviewTargetQueryAdapter implements CompanionReviewTargetQueryPort {

	private final CompanionReviewTargetQueryJpaRepository repository;
	private final CompanionPlaceTimeResolver placeTimeResolver;
	private final Clock clock;

	public CompanionReviewTargetQueryAdapter(final CompanionReviewTargetQueryJpaRepository repository) {
		this(repository, new CompanionPlaceTimeResolver(), Clock.systemUTC());
	}

	@Autowired
	public CompanionReviewTargetQueryAdapter(
			final CompanionReviewTargetQueryJpaRepository repository,
			final CompanionPlaceTimeResolver placeTimeResolver,
			final Clock clock
	) {
		this.repository = repository;
		this.placeTimeResolver = placeTimeResolver;
		this.clock = clock;
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
				CompanionPlaceCityNameResolver.resolve(row.getPlaceAddress()),
				row.getMeetingAt().atOffset(ZoneOffset.UTC).atZoneSameInstant(
						placeTimeResolver.resolveZone(row.getPlaceAddress(), row.getLatitude(),
								row.getLongitude(), clock.instant())).toLocalDate(),
				Boolean.TRUE.equals(row.getCheckedIn()),
				Boolean.TRUE.equals(row.getHasWrittenReview())
		);
	}
}
