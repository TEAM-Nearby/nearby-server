// Google Places 조회 포트 응답을 Redis TTL 캐시로 감싸는 어댑터
package com.sopt.nearby.place.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.PlaceImageLookupPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsResult;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchRequest;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public class RedisGooglePlacesCacheAdapter
		implements SoloDiningPlaceSearchPort, SoloDiningPlaceDetailsPort, PlaceImageLookupPort {

	private static final Logger log = LoggerFactory.getLogger(RedisGooglePlacesCacheAdapter.class);
	private static final String KEY_PREFIX = "nearby:google-places:";
	private static final Duration SEARCH_TTL = Duration.ofMinutes(5);
	private static final Duration DETAILS_TTL = Duration.ofHours(24);
	private static final Duration IMAGE_TTL = Duration.ofHours(1);
	private static final TypeReference<List<SoloDiningPlaceSearchResult>> SEARCH_RESULTS =
			new TypeReference<>() {
			};

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final SoloDiningPlaceSearchPort searchPort;
	private final SoloDiningPlaceDetailsPort detailsPort;
	private final PlaceImageLookupPort imageLookupPort;

	public RedisGooglePlacesCacheAdapter(
			final StringRedisTemplate redisTemplate,
			final ObjectMapper objectMapper,
			@Qualifier("googleSoloDiningPlaceSearchAdapter") final SoloDiningPlaceSearchPort searchPort,
			@Qualifier("googleSoloDiningPlaceDetailsAdapter") final SoloDiningPlaceDetailsPort detailsPort,
			@Qualifier("googlePlacesImageLookupAdapter") final PlaceImageLookupPort imageLookupPort
	) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.searchPort = searchPort;
		this.detailsPort = detailsPort;
		this.imageLookupPort = imageLookupPort;
	}

	@Override
	public List<SoloDiningPlaceSearchResult> search(final SoloDiningPlaceSearchRequest request) {
		String key = KEY_PREFIX + "search:" + sha256(searchKey(request));
		Optional<List<SoloDiningPlaceSearchResult>> cached = read(key, SEARCH_RESULTS);
		if (cached.isPresent()) {
			return cached.get();
		}
		List<SoloDiningPlaceSearchResult> result = searchPort.search(request);
		write(key, result, SEARCH_TTL);
		return result;
	}

	@Override
	public SoloDiningPlaceDetailsResult findByGooglePlaceId(final String googlePlaceId) {
		String key = KEY_PREFIX + "details:" + googlePlaceId;
		Optional<SoloDiningPlaceDetailsResult> cached = read(key, SoloDiningPlaceDetailsResult.class);
		if (cached.isPresent()) {
			return cached.get();
		}
		SoloDiningPlaceDetailsResult result = detailsPort.findByGooglePlaceId(googlePlaceId);
		if (result != null) {
			write(key, result, DETAILS_TTL);
		}
		return result;
	}

	@Override
	public Optional<ResolvedPlaceImage> findImage(final String googlePlaceId, final String photoReference) {
		String key = KEY_PREFIX + "image:" + sha256(String.valueOf(googlePlaceId) + "|" + photoReference);
		Optional<ResolvedPlaceImage> cached = read(key, ResolvedPlaceImage.class);
		if (cached.isPresent()) {
			return cached;
		}
		Optional<ResolvedPlaceImage> result = imageLookupPort.findImage(googlePlaceId, photoReference);
		result.ifPresent(image -> write(key, image, IMAGE_TTL));
		return result;
	}

	private <T> Optional<T> read(final String key, final Class<T> type) {
		try {
			String value = redisTemplate.opsForValue().get(key);
			return value == null ? Optional.empty() : Optional.ofNullable(objectMapper.readValue(value, type));
		} catch (JsonProcessingException | DataAccessException exception) {
			log.warn("Google Places cache read failed. key={}", key, exception);
			return Optional.empty();
		}
	}

	private <T> Optional<T> read(final String key, final TypeReference<T> type) {
		try {
			String value = redisTemplate.opsForValue().get(key);
			return value == null ? Optional.empty() : Optional.ofNullable(objectMapper.readValue(value, type));
		} catch (JsonProcessingException | DataAccessException exception) {
			log.warn("Google Places cache read failed. key={}", key, exception);
			return Optional.empty();
		}
	}

	private void write(final String key, final Object value, final Duration ttl) {
		try {
			redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
		} catch (JsonProcessingException | DataAccessException exception) {
			log.warn("Google Places cache write failed. key={}", key, exception);
		}
	}

	private String searchKey(final SoloDiningPlaceSearchRequest request) {
		return request.latitude().toPlainString()
				+ "|" + request.longitude().toPlainString()
				+ "|" + request.radiusMeters()
				+ "|" + request.maxResultCount()
				+ "|" + String.join(",", request.includedTypes());
	}

	private String sha256(final String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm is unavailable.", exception);
		}
	}
}
