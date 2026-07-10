// Google Places 포트 Redis 캐시 어댑터의 hit, miss, TTL 동작을 검증한다.
package com.sopt.nearby.place.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.PlaceImageLookupPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsResult;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchRequest;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchResult;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisGooglePlacesCacheAdapterTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void cachesSearchResultForFiveMinutesOnMiss() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(null);
		FakeSearchPort searchPort = new FakeSearchPort(List.of(searchResult()));
		RedisGooglePlacesCacheAdapter adapter = newAdapter(searchPort, new FakeDetailsPort(null), new FakeImagePort(Optional.empty()));

		List<SoloDiningPlaceSearchResult> result = adapter.search(searchRequest());

		assertThat(result).containsExactly(searchResult());
		assertThat(searchPort.calls).isEqualTo(1);
		ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
		verify(valueOperations).set(anyString(), value.capture(), org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(5)));
		assertThat(value.getValue()).contains("google-place-id");
	}

	@Test
	void returnsCachedDetailsWithoutCallingDelegate() throws Exception {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("nearby:google-places:details:google-place-id"))
				.thenReturn(objectMapper.writeValueAsString(detailsResult()));
		FakeDetailsPort detailsPort = new FakeDetailsPort(detailsResult());
		RedisGooglePlacesCacheAdapter adapter = newAdapter(new FakeSearchPort(List.of()), detailsPort, new FakeImagePort(Optional.empty()));

		SoloDiningPlaceDetailsResult result = adapter.findByGooglePlaceId("google-place-id");

		assertThat(result).isEqualTo(detailsResult());
		assertThat(detailsPort.calls).isZero();
		verify(valueOperations, never()).set(anyString(), anyString(), org.mockito.ArgumentMatchers.any(Duration.class));
	}

	@Test
	void cachesImageResultForOneHourOnMiss() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString())).thenReturn(null);
		FakeImagePort imagePort = new FakeImagePort(Optional.of(imageResult()));
		RedisGooglePlacesCacheAdapter adapter = newAdapter(new FakeSearchPort(List.of()), new FakeDetailsPort(null), imagePort);

		Optional<ResolvedPlaceImage> result = adapter.findImage("google-place-id", "photo-reference");

		assertThat(result).contains(imageResult());
		assertThat(imagePort.calls).isEqualTo(1);
		verify(valueOperations).set(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(Duration.ofHours(1)));
	}

	@Test
	void fallsBackToDelegateWhenCachedDetailsIsCorrupted() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("nearby:google-places:details:google-place-id")).thenReturn("{");
		FakeDetailsPort detailsPort = new FakeDetailsPort(detailsResult());
		RedisGooglePlacesCacheAdapter adapter = newAdapter(
				new FakeSearchPort(List.of()),
				detailsPort,
				new FakeImagePort(Optional.empty())
		);

		SoloDiningPlaceDetailsResult result = adapter.findByGooglePlaceId("google-place-id");

		assertThat(result).isEqualTo(detailsResult());
		assertThat(detailsPort.calls).isEqualTo(1);
	}

	@Test
	void fallsBackToDelegateWhenRedisReadFails() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("nearby:google-places:details:google-place-id"))
				.thenThrow(new RedisConnectionFailureException("failed"));
		FakeDetailsPort detailsPort = new FakeDetailsPort(detailsResult());
		RedisGooglePlacesCacheAdapter adapter = newAdapter(
				new FakeSearchPort(List.of()),
				detailsPort,
				new FakeImagePort(Optional.empty())
		);

		SoloDiningPlaceDetailsResult result = adapter.findByGooglePlaceId("google-place-id");

		assertThat(result).isEqualTo(detailsResult());
		assertThat(detailsPort.calls).isEqualTo(1);
	}

	@Test
	void returnsDelegateResultWhenSerializationFails() throws Exception {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("nearby:google-places:details:google-place-id")).thenReturn(null);
		ObjectMapper failingObjectMapper = mock(ObjectMapper.class);
		when(failingObjectMapper.writeValueAsString(any()))
				.thenThrow(new JsonProcessingException("failed") {
				});
		FakeDetailsPort detailsPort = new FakeDetailsPort(detailsResult());
		RedisGooglePlacesCacheAdapter adapter = new RedisGooglePlacesCacheAdapter(
				redisTemplate,
				failingObjectMapper,
				new FakeSearchPort(List.of()),
				detailsPort,
				new FakeImagePort(Optional.empty())
		);

		SoloDiningPlaceDetailsResult result = adapter.findByGooglePlaceId("google-place-id");

		assertThat(result).isEqualTo(detailsResult());
		assertThat(detailsPort.calls).isEqualTo(1);
	}

	@Test
	void returnsDelegateResultWhenRedisWriteFails() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("nearby:google-places:details:google-place-id")).thenReturn(null);
		doThrow(new RedisConnectionFailureException("failed"))
				.when(valueOperations)
				.set(anyString(), anyString(), eq(Duration.ofHours(24)));
		FakeDetailsPort detailsPort = new FakeDetailsPort(detailsResult());
		RedisGooglePlacesCacheAdapter adapter = newAdapter(
				new FakeSearchPort(List.of()),
				detailsPort,
				new FakeImagePort(Optional.empty())
		);

		SoloDiningPlaceDetailsResult result = adapter.findByGooglePlaceId("google-place-id");

		assertThat(result).isEqualTo(detailsResult());
		assertThat(detailsPort.calls).isEqualTo(1);
	}

	private RedisGooglePlacesCacheAdapter newAdapter(
			final SoloDiningPlaceSearchPort searchPort,
			final SoloDiningPlaceDetailsPort detailsPort,
			final PlaceImageLookupPort imagePort
	) {
		return new RedisGooglePlacesCacheAdapter(redisTemplate, objectMapper, searchPort, detailsPort, imagePort);
	}

	private SoloDiningPlaceSearchRequest searchRequest() {
		return new SoloDiningPlaceSearchRequest(
				new BigDecimal("37.50000000"),
				new BigDecimal("127.00000000"),
				1000,
				20,
				List.of("restaurant")
		);
	}

	private SoloDiningPlaceSearchResult searchResult() {
		return new SoloDiningPlaceSearchResult(
				"google-place-id",
				"혼밥집",
				"서울시",
				new BigDecimal("37.50000000"),
				new BigDecimal("127.00000000"),
				SoloDiningPlaceCategory.RESTAURANT,
				new BigDecimal("4.5"),
				12,
				"photo-reference",
				PlaceBusinessStatus.OPERATIONAL
		);
	}

	private SoloDiningPlaceDetailsResult detailsResult() {
		return new SoloDiningPlaceDetailsResult(
				"google-place-id",
				"혼밥집",
				"서울시",
				new BigDecimal("37.50000000"),
				new BigDecimal("127.00000000"),
				SoloDiningPlaceCategory.RESTAURANT,
				new BigDecimal("4.5"),
				12,
				"021234567",
				"photo-reference",
				List.of("photo-reference"),
				PlaceBusinessStatus.OPERATIONAL,
				null,
				null,
				List.of("월요일: 10:00~20:00"),
				"좋은 곳"
		);
	}

	private ResolvedPlaceImage imageResult() {
		return new ResolvedPlaceImage(
				"https://image.example/place.jpg",
				ResolvedPlaceImage.GOOGLE_MAPS,
				List.of(new ResolvedPlaceImage.ImageAttribution("작가", "https://author.example", null))
		);
	}

	private static final class FakeSearchPort implements SoloDiningPlaceSearchPort {

		private final List<SoloDiningPlaceSearchResult> result;
		private int calls;

		private FakeSearchPort(final List<SoloDiningPlaceSearchResult> result) {
			this.result = result;
		}

		@Override
		public List<SoloDiningPlaceSearchResult> search(final SoloDiningPlaceSearchRequest request) {
			calls++;
			return result;
		}
	}

	private static final class FakeDetailsPort implements SoloDiningPlaceDetailsPort {

		private final SoloDiningPlaceDetailsResult result;
		private int calls;

		private FakeDetailsPort(final SoloDiningPlaceDetailsResult result) {
			this.result = result;
		}

		@Override
		public SoloDiningPlaceDetailsResult findByGooglePlaceId(final String googlePlaceId) {
			calls++;
			return result;
		}
	}

	private static final class FakeImagePort implements PlaceImageLookupPort {

		private final Optional<ResolvedPlaceImage> result;
		private int calls;

		private FakeImagePort(final Optional<ResolvedPlaceImage> result) {
			this.result = result;
		}

		@Override
		public Optional<ResolvedPlaceImage> findImage(final String googlePlaceId, final String photoReference) {
			calls++;
			return result;
		}
	}
}
