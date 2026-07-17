// 리프레시 토큰 도메인 저장소 포트를 Redis TTL key로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

	private static final String KEY_PREFIX = "nearby:refresh-token:";
	private static final String VALUE_SEPARATOR = "|";
	private static final DefaultRedisScript<Long> REVOKE_IF_UNCHANGED_SCRIPT = new DefaultRedisScript<>("""
			if redis.call('GET', KEYS[1]) ~= ARGV[1] then
				return 0
			end
			redis.call('PSETEX', KEYS[1], ARGV[3], ARGV[2])
			return 1
			""", Long.class);

	private final StringRedisTemplate redisTemplate;
	private final Clock clock;

	@Autowired
	public RefreshTokenRepositoryAdapter(final StringRedisTemplate redisTemplate, final Clock clock) {
		this.redisTemplate = redisTemplate;
		this.clock = clock;
	}

	@Override
	public RefreshToken save(final RefreshToken model) {
		Duration ttl = Duration.between(LocalDateTime.now(clock), model.expiresAt());
		if (ttl.isNegative() || ttl.isZero()) {
			redisTemplate.delete(key(model.tokenHash()));
			return model;
		}
		redisTemplate.opsForValue().set(
				key(model.tokenHash()),
				value(model.userId(), model.expiresAt(), model.revokedAt()),
				ttl
		);
		return model;
	}

	@Override
	public Optional<RefreshToken> findByTokenHash(final String tokenHash) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key(tokenHash)))
				.flatMap(value -> toRefreshToken(tokenHash, value));
	}

	@Override
	public boolean revokeByTokenHashIfActive(
			final String tokenHash,
			final Long userId,
			final LocalDateTime revokedAt
	) {
		LocalDateTime now = LocalDateTime.now(clock);
		String redisKey = key(tokenHash);
		String currentValue = redisTemplate.opsForValue().get(redisKey);
		if (currentValue == null) {
			return false;
		}
		Optional<RefreshToken> found = toRefreshToken(tokenHash, currentValue);
		if (found.isEmpty()) {
			return false;
		}
		RefreshToken token = found.get();
		if (!token.userId().equals(userId) || token.revokedAt() != null || !token.expiresAt().isAfter(now)) {
			return false;
		}
		long ttlMillis = Duration.between(now, token.expiresAt()).toMillis();
		if (ttlMillis <= 0) {
			return false;
		}
		Long result = redisTemplate.execute(
				REVOKE_IF_UNCHANGED_SCRIPT,
				List.of(redisKey),
				currentValue,
				value(token.userId(), token.expiresAt(), revokedAt),
				Long.toString(ttlMillis)
		);
		return Long.valueOf(1L).equals(result);
	}

	private Optional<RefreshToken> toRefreshToken(final String tokenHash, final String value) {
		String[] parts = value.split("\\|", -1);
		if (parts.length < 2 || parts.length > 3) {
			return Optional.empty();
		}
		try {
			return Optional.of(new RefreshToken(
					null,
					Long.valueOf(parts[0]),
					tokenHash,
					LocalDateTime.parse(parts[1]),
					parts.length == 3 ? LocalDateTime.parse(parts[2]) : null
			));
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}

	private String value(final Long userId, final LocalDateTime expiresAt, final LocalDateTime revokedAt) {
		if (revokedAt == null) {
			return userId + VALUE_SEPARATOR + expiresAt;
		}
		return userId + VALUE_SEPARATOR + expiresAt + VALUE_SEPARATOR + revokedAt;
	}

	private String key(final String tokenHash) {
		return KEY_PREFIX + tokenHash;
	}
}
