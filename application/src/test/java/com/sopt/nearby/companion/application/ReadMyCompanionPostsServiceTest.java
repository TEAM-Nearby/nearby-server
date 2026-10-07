// 내가 작성한 동행 모집글 목록 조회 서비스의 응답 매핑을 검증하는 테스트다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import com.sopt.nearby.companion.domain.model.post.MyCompanionPostSummary;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.port.out.MyCompanionPostQueryPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ReadMyCompanionPostsServiceTest {
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-01T12:00:00Z"), ZoneOffset.UTC);

	private FakeMyCompanionPostQueryPort queryPort;
	private ReadMyCompanionPostsService service;

	@BeforeEach
	void setUp() {
		queryPort = new FakeMyCompanionPostQueryPort();
		service = new ReadMyCompanionPostsService(queryPort, CLOCK);
	}

	@Test
	void returnsMyCompanionPostsWithKoreanCityNameAndGooglePlaceId() {
		queryPort.posts = List.of(post(
				"Pasadizo de San Gines, 5, Madrid, Spain",
				List.of(ReviewKeyword.PUNCTUAL, ReviewKeyword.GOOD_MANNERS)
		));

		ReadMyCompanionPostsResult result = service.getPosts(1L);

		assertEquals(1L, queryPort.hostUserId);
		assertEquals(1, result.posts().size());
		ReadMyCompanionPostsResult.Post post = result.posts().get(0);
		assertEquals(10L, post.postId());
		assertEquals("마드리드", post.cityNameKor());
		assertEquals(CompanionCity.MADRID, post.city());
		assertEquals("2026-07-01T14:00+02:00", post.currentLocalTime().toOffsetDateTime().toString());
		assertEquals(LocalDateTime.of(2026, 6, 29, 19, 0), post.scheduledAt());
		assertEquals("google-place-id", post.place().googlePlaceId());
		assertEquals("시우다드 콘달", post.place().name());
		assertEquals(new BigDecimal("41.39020500"), post.place().latitude());
		assertEquals(new BigDecimal("2.16354800"), post.place().longitude());
		assertEquals("https://cdn.nearby.com/profiles/1.jpg", post.hostProfileImageUrl());
		assertEquals(List.of(
				new ReadMyCompanionPostsResult.Member(2L, "https://cdn.nearby.com/profiles/2.jpg"),
				new ReadMyCompanionPostsResult.Member(3L, null)
		), post.members());
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

	@ParameterizedTest
	@CsvSource({
			"'Rambla de Catalunya, 18, Barcelona, Spain', BARCELONA, 바르셀로나",
			"'Pasadizo de San Gines, 5, Madrid, Spain', MADRID, 마드리드",
			"'Madrid Road, London, UK', LONDON, 런던",
			"'10 Rue de Rivoli, Paris, France', PARIS, 파리"
	})
	void returnsKoreanNameForSupportedCity(String address, CompanionCity city, String koreanName) {
		queryPort.posts = List.of(post(address, List.of()));

		ReadMyCompanionPostsResult.Post result = service.getPosts(1L).posts().get(0);

		assertEquals(city, result.city());
		assertEquals(koreanName, result.cityNameKor());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "Via dei Giubbonari, 21, Rome, Italy"})
	void returnsNullCityFieldsWhenCityCannotBeResolved(String address) {
		queryPort.posts = List.of(post(address, List.of()));

		ReadMyCompanionPostsResult result = service.getPosts(1L);

		assertNull(result.posts().get(0).cityNameKor());
		assertNull(result.posts().get(0).city());
		assertNull(result.posts().get(0).currentLocalTime());
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
				"https://cdn.nearby.com/profiles/1.jpg",
				List.of(
						new MyCompanionPostSummary.Member(2L, "https://cdn.nearby.com/profiles/2.jpg"),
						new MyCompanionPostSummary.Member(3L, null)
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
