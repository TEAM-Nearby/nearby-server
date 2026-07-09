// 리프레시 토큰 도메인 저장소 포트를 Redis TTL key로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.port.out.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

	private static final String KEY_PREFIX = "nearby:refresh-token:";
	private static final String VALUE_SEPARATOR = "|";

	private final StringRedisTemplate redisTemplate;
	private final Clock clock;

	@Autowired
	public RefreshTokenRepositoryAdapter(final StringRedisTemplate redisTemplate) {
		this(redisTemplate, Clock.systemUTC());
	}

	RefreshTokenRepositoryAdapter(final StringRedisTemplate redisTemplate, final Clock clock) {
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
		return findByTokenHash(tokenHash)
				.filter(token -> token.userId().equals(userId))
				.filter(token -> token.revokedAt() == null)
				.filter(token -> token.expiresAt().isAfter(now))
				.map(token -> {
					redisTemplate.opsForValue().set(
							key(tokenHash),
							value(token.userId(), token.expiresAt(), revokedAt),
							Duration.between(now, token.expiresAt())
					);
					return true;
				})
				.orElse(false);
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
