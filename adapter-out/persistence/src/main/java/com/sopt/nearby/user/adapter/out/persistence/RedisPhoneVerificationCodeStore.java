// 휴대폰 인증 코드 해시를 Redis TTL key로 저장하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.user.port.out.PhoneVerificationCodeStore;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisPhoneVerificationCodeStore implements PhoneVerificationCodeStore {

	private static final String KEY_PREFIX = "nearby:phone-verification:";

	private final StringRedisTemplate redisTemplate;

	public RedisPhoneVerificationCodeStore(final StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void save(final Long phoneVerificationId, final String verificationCodeHash, final Duration ttl) {
		redisTemplate.opsForValue().set(key(phoneVerificationId), verificationCodeHash, ttl);
	}

	@Override
	public Optional<String> findHash(final Long phoneVerificationId) {
		return Optional.ofNullable(redisTemplate.opsForValue().get(key(phoneVerificationId)));
	}

	@Override
	public void delete(final Long phoneVerificationId) {
		redisTemplate.delete(key(phoneVerificationId));
	}

	private String key(final Long phoneVerificationId) {
		return KEY_PREFIX + phoneVerificationId;
	}
}
