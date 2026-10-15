// 휴대폰 인증 코드 Redis 저장소 어댑터의 key와 TTL 동작을 검증한다.
package com.sopt.nearby.user.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisPhoneVerificationCodeStoreTest {

	private static final String KEY = "nearby:phone-verification:10";

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Test
	void savesCodeHashWithTtl() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		RedisPhoneVerificationCodeStore store = new RedisPhoneVerificationCodeStore(redisTemplate);

		store.save(10L, "hash", Duration.ofSeconds(180));

		verify(valueOperations).set(KEY, "hash", Duration.ofSeconds(180));
	}

	@Test
	void findsCodeHash() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(KEY)).thenReturn("hash");
		RedisPhoneVerificationCodeStore store = new RedisPhoneVerificationCodeStore(redisTemplate);

		Optional<String> result = store.findHash(10L);

		assertThat(result).contains("hash");
	}

	@Test
	void deletesCodeHash() {
		RedisPhoneVerificationCodeStore store = new RedisPhoneVerificationCodeStore(redisTemplate);

		store.delete(10L);

		verify(redisTemplate).delete(KEY);
	}
}
