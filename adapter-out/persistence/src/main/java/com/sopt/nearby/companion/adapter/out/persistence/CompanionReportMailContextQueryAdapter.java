// 동행 신고 이메일 조회 결과를 애플리케이션 모델로 변환하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReportMailContextProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReportMailContextQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.port.out.CompanionReportMailContext;
import com.sopt.nearby.companion.port.out.CompanionReportMailContextQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionReportMailContextQueryAdapter implements CompanionReportMailContextQueryPort {

	private final CompanionReportMailContextQueryJpaRepository repository;

	public CompanionReportMailContextQueryAdapter(
			final CompanionReportMailContextQueryJpaRepository repository
	) {
		this.repository = repository;
	}

	@Override
	public Optional<CompanionReportMailContext> findByMeetingIdAndUserIds(
			final Long meetingId,
			final Long reporterUserId,
			final Long reportedUserId
	) {
		return repository.findByMeetingIdAndUserIds(meetingId, reporterUserId, reportedUserId)
				.map(this::toContext);
	}

	private CompanionReportMailContext toContext(final CompanionReportMailContextProjection row) {
		return new CompanionReportMailContext(
				row.getReporterNickname(),
				row.getReporterPhoneNumber(),
				row.getReportedNickname(),
				row.getReportedPhoneNumber(),
				row.getCompanionPostId(),
				row.getCompanionPostContent(),
				row.getScheduledAt(),
				CompanionPostMeetingTimeType.valueOf(row.getMeetingTimeType()),
				row.getPlaceAddress()
		);
	}
}
