// 내가 작성한 동행 모집글 목록 조회 유스케이스를 구현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver.ResolvedCityTime;
import com.sopt.nearby.companion.domain.model.post.MyCompanionPostSummary;
import com.sopt.nearby.companion.port.in.ReadMyCompanionPostsUseCase;
import com.sopt.nearby.companion.port.out.MyCompanionPostQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class ReadMyCompanionPostsService implements ReadMyCompanionPostsUseCase {

	private final MyCompanionPostQueryPort queryPort;
	private final Clock clock;

	public ReadMyCompanionPostsService(final MyCompanionPostQueryPort queryPort, final Clock clock) {
		this.queryPort = queryPort;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public ReadMyCompanionPostsResult getPosts(final Long userId) {
		final Instant now = clock.instant();
		List<ReadMyCompanionPostsResult.Post> posts = queryPort.findAllByHostUserId(userId)
				.stream()
				.map(summary -> toPost(summary, now))
				.toList();
		return new ReadMyCompanionPostsResult(posts);
	}

	private ReadMyCompanionPostsResult.Post toPost(final MyCompanionPostSummary summary, final Instant now) {
		final ResolvedCityTime cityTime = CompanionPlaceCityNameResolver.resolveCurrentTime(
				summary.place().address(),
				now
		);
		return new ReadMyCompanionPostsResult.Post(
				summary.postId(),
				CompanionPlaceCityNameResolver.resolve(summary.place().address(), summary.place().name()),
				cityTime.city(),
				cityTime.currentLocalTime(),
				summary.scheduledAt(),
				new ReadMyCompanionPostsResult.Place(
						summary.place().googlePlaceId(),
						summary.place().name(),
						summary.place().latitude(),
						summary.place().longitude()
				),
				summary.hostProfileImageUrl(),
				summary.members().stream()
						.map(member -> new ReadMyCompanionPostsResult.Member(
								member.userId(),
								member.profileImageUrl()
						))
						.toList(),
				summary.currentParticipants(),
				summary.maxParticipants(),
				summary.content(),
				summary.reviewKeywords()
		);
	}
}
