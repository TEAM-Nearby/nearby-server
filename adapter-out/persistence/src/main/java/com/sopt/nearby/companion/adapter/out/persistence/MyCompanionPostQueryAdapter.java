// 내가 작성한 동행 모집글 목록 조회 쿼리 포트를 JPA로 구현한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.MyCompanionPostKeywordProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MyCompanionPostProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MyCompanionPostQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.post.MyCompanionPostSummary;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.port.out.MyCompanionPostQueryPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MyCompanionPostQueryAdapter implements MyCompanionPostQueryPort {

	private final MyCompanionPostQueryJpaRepository repository;

	public MyCompanionPostQueryAdapter(final MyCompanionPostQueryJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<MyCompanionPostSummary> findAllByHostUserId(final Long hostUserId) {
		List<MyCompanionPostProjection> rows = repository.findAllByHostUserId(hostUserId);
		if (rows.isEmpty()) {
			return List.of();
		}

		Map<Long, List<ReviewKeyword>> reviewKeywords = reviewKeywords(rows, hostUserId);
		return rows.stream()
				.map(row -> toSummary(row, reviewKeywords.getOrDefault(row.getPostId(), List.of())))
				.toList();
	}

	private Map<Long, List<ReviewKeyword>> reviewKeywords(
			final List<MyCompanionPostProjection> rows,
			final Long hostUserId
	) {
		List<Long> postIds = rows.stream()
				.map(MyCompanionPostProjection::getPostId)
				.toList();
		Map<Long, List<ReviewKeyword>> keywordsByPostId = new LinkedHashMap<>();
		for (MyCompanionPostKeywordProjection row : repository.findReviewKeywordsByPostIds(postIds, hostUserId)) {
			keywordsByPostId
					.computeIfAbsent(row.getPostId(), ignored -> new ArrayList<>())
					.add(ReviewKeyword.valueOf(row.getKeyword()));
		}
		return keywordsByPostId;
	}

	private MyCompanionPostSummary toSummary(
			final MyCompanionPostProjection row,
			final List<ReviewKeyword> reviewKeywords
	) {
		return new MyCompanionPostSummary(
				row.getPostId(),
				row.getScheduledAt(),
				new MyCompanionPostSummary.Place(
						row.getGooglePlaceId(),
						row.getPlaceName(),
						row.getPlaceAddress(),
						row.getLatitude(),
						row.getLongitude()
				),
				row.getCurrentParticipants().intValue(),
				row.getMaxParticipants().intValue(),
				row.getContent(),
				reviewKeywords
		);
	}
}
