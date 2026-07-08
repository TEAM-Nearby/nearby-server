// 내가 작성한 동행 모집글 목록 조회 서비스의 응답 매핑을 검증하는 테스트다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sopt.nearby.companion.domain.model.post.MyCompanionPostSummary;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.port.out.MyCompanionPostQueryPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadMyCompanionPostsServiceTest {

	private FakeMyCompanionPostQueryPort queryPort;
	private ReadMyCompanionPostsService service;

	@BeforeEach
	void setUp() {
		queryPort = new FakeMyCompanionPostQueryPort();
		service = new ReadMyCompanionPostsService(queryPort);
	}

	@Test
	void returnsMyCompanionPostsWithCityNameAndGooglePlaceId() {
		queryPort.posts = List.of(post(
				"바르셀로나 Rambla de Catalunya, 16",
				List.of(ReviewKeyword.PUNCTUAL, ReviewKeyword.GOOD_MANNERS)
		));

		ReadMyCompanionPostsResult result = service.getPosts(1L);

		assertEquals(1L, queryPort.hostUserId);
		assertEquals(1, result.posts().size());
		ReadMyCompanionPostsResult.Post post = result.posts().get(0);
		assertEquals(10L, post.postId());
		assertEquals("바르셀로나", post.cityName());
		assertEquals(LocalDateTime.of(2026, 6, 29, 19, 0), post.scheduledAt());
		assertEquals("google-place-id", post.place().googlePlaceId());
		assertEquals("시우다드 콘달", post.place().name());
		assertEquals(new BigDecimal("41.39020500"), post.place().latitude());
		assertEquals(new BigDecimal("2.16354800"), post.place().longitude());
		assertEquals(3, post.currentParticipants());
		assertEquals(4, post.maxParticipants());
		assertEquals("같이 밥 먹어요.", post.content());
		assertEquals(List.of(ReviewKeyword.PUNCTUAL, ReviewKeyword.GOOD_MANNERS), post.reviewKeywords());
	}

	@Test
	void returnsEmptyPosts() {
		queryPort.posts = List.of();

		ReadMyCompanionPostsResult result = service.getPosts(1L);

		assertEquals(List.of(), result.posts());
	}

	@Test
	void usesPlaceNameWhenAddressIsBlank() {
		queryPort.posts = List.of(post(" ", List.of()));

		ReadMyCompanionPostsResult result = service.getPosts(1L);

		assertEquals("시우다드 콘달", result.posts().get(0).cityName());
	}

	private MyCompanionPostSummary post(
			final String address,
			final List<ReviewKeyword> reviewKeywords
	) {
		return new MyCompanionPostSummary(
				10L,
				LocalDateTime.of(2026, 6, 29, 19, 0),
				new MyCompanionPostSummary.Place(
						"google-place-id",
						"시우다드 콘달",
						address,
						new BigDecimal("41.39020500"),
						new BigDecimal("2.16354800")
				),
				3,
				4,
				"같이 밥 먹어요.",
				reviewKeywords
		);
	}

	private static final class FakeMyCompanionPostQueryPort implements MyCompanionPostQueryPort {

		private Long hostUserId;
		private List<MyCompanionPostSummary> posts = List.of();

		@Override
		public List<MyCompanionPostSummary> findAllByHostUserId(final Long hostUserId) {
			this.hostUserId = hostUserId;
			return posts;
		}
	}
}
