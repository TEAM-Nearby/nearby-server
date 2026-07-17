// 리프레시 토큰 Redis 저장소 어댑터의 TTL 저장과 만료 처리를 검증한다.
package com.sopt.nearby.user.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sopt.nearby.user.domain.model.RefreshToken;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenRepositoryAdapterTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-07T03:00:00Z"), ZoneId.of("Asia/Seoul"));
	private static final String KEY = "nearby:refresh-token:token-hash";

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Test
	void savesRefreshTokenWithTtl() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		RefreshTokenRepositoryAdapter adapter = new RefreshTokenRepositoryAdapter(redisTemplate, CLOCK);
		RefreshToken token = new RefreshToken(
				null,
				7L,
				"token-hash",
				LocalDateTime.of(2026, 7, 8, 12, 0),
				null
		);

		RefreshToken saved = adapter.save(token);

		assertThat(saved).isEqualTo(token);
		verify(valueOperations).set(KEY, "7|2026-07-08T12:00", Duration.ofDays(1));
	}

	@Test
	void findsRefreshTokenByTokenHash() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(KEY)).thenReturn("7|2026-07-08T12:00");
		RefreshTokenRepositoryAdapter adapter = new RefreshTokenRepositoryAdapter(redisTemplate, CLOCK);

		Optional<RefreshToken> found = adapter.findByTokenHash("token-hash");

		assertThat(found).contains(new RefreshToken(null, 7L, "token-hash", LocalDateTime.of(2026, 7, 8, 12, 0), null));
	}

	@Test
	void findsRevokedRefreshTokenByTokenHash() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(KEY)).thenReturn("7|2026-07-08T12:00|2026-07-07T12:00");
		RefreshTokenRepositoryAdapter adapter = new RefreshTokenRepositoryAdapter(redisTemplate, CLOCK);

		Optional<RefreshToken> found = adapter.findByTokenHash("token-hash");

		assertThat(found).contains(new RefreshToken(
				null,
				7L,
				"token-hash",
				LocalDateTime.of(2026, 7, 8, 12, 0),
				LocalDateTime.of(2026, 7, 7, 12, 0)
		));
	}

	@Test
	void marksRefreshTokenRevokedWithRemainingTtlWhenItIsActiveAndOwnedByUser() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		String activeValue = "7|2026-07-08T12:00";
		String revokedValue = "7|2026-07-08T12:00|2026-07-07T12:00";
		when(valueOperations.get(KEY)).thenReturn(activeValue);
		when(redisTemplate.execute(
				any(),
				eq(List.of(KEY)),
				eq(activeValue),
				eq(revokedValue),
				eq("86400000")
		)).thenReturn(1L);
		RefreshTokenRepositoryAdapter adapter = new RefreshTokenRepositoryAdapter(redisTemplate, CLOCK);

		boolean revoked = adapter.revokeByTokenHashIfActive(
				"token-hash",
				7L,
				LocalDateTime.of(2026, 7, 7, 12, 0)
		);

		assertThat(revoked).isTrue();
		verify(redisTemplate).execute(
				any(),
				eq(List.of(KEY)),
				eq(activeValue),
				eq(revokedValue),
				eq("86400000")
		);
	}

	@Test
	void returnsFalseWhenCompareAndSetLosesRace() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		String activeValue = "7|2026-07-08T12:00";
		String revokedValue = "7|2026-07-08T12:00|2026-07-07T12:00";
		when(valueOperations.get(KEY)).thenReturn(activeValue);
		when(redisTemplate.execute(
				any(),
				eq(List.of(KEY)),
				eq(activeValue),
				eq(revokedValue),
				eq("86400000")
		)).thenReturn(0L);
		RefreshTokenRepositoryAdapter adapter = new RefreshTokenRepositoryAdapter(redisTemplate, CLOCK);

		boolean revoked = adapter.revokeByTokenHashIfActive(
				"token-hash",
				7L,
				LocalDateTime.of(2026, 7, 7, 12, 0)
		);

		assertThat(revoked).isFalse();
	}

	@Test
	void doesNotDeleteRefreshTokenForDifferentUser() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(KEY)).thenReturn("7|2026-07-08T12:00");
		RefreshTokenRepositoryAdapter adapter = new RefreshTokenRepositoryAdapter(redisTemplate, CLOCK);

		boolean revoked = adapter.revokeByTokenHashIfActive(
				"token-hash",
				99L,
				LocalDateTime.of(2026, 7, 7, 12, 0)
		);

		assertThat(revoked).isFalse();
		verify(redisTemplate, never()).execute(any(), anyList(), any(Object[].class));
	}

	@Test
	void doesNotRevokeAlreadyRevokedRefreshToken() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(KEY)).thenReturn("7|2026-07-08T12:00|2026-07-06T12:00");
		RefreshTokenRepositoryAdapter adapter = new RefreshTokenRepositoryAdapter(redisTemplate, CLOCK);

		boolean revoked = adapter.revokeByTokenHashIfActive(
				"token-hash",
				7L,
				LocalDateTime.of(2026, 7, 7, 12, 0)
		);

		assertThat(revoked).isFalse();
		verify(redisTemplate, never()).execute(any(), anyList(), any(Object[].class));
	}

	@Test
	void doesNotRevokeExpiredRefreshToken() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(KEY)).thenReturn("7|2026-07-07T11:59");
		RefreshTokenRepositoryAdapter adapter = new RefreshTokenRepositoryAdapter(redisTemplate, CLOCK);

		boolean revoked = adapter.revokeByTokenHashIfActive(
				"token-hash",
				7L,
				LocalDateTime.of(2026, 7, 7, 12, 0)
		);

		assertThat(revoked).isFalse();
		verify(redisTemplate, never()).execute(any(), anyList(), any(Object[].class));
	}

	@Test
	void deletesRefreshTokenWhenSaveTtlIsNotPositive() {
		RefreshTokenRepositoryAdapter adapter = new RefreshTokenRepositoryAdapter(redisTemplate, CLOCK);
		RefreshToken token = new RefreshToken(
				null,
				7L,
				"token-hash",
				LocalDateTime.of(2026, 7, 7, 12, 0),
				null
		);

		RefreshToken saved = adapter.save(token);

		assertThat(saved).isEqualTo(token);
		verify(redisTemplate).delete(KEY);
	}
}
