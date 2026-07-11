// Nearby 서버 애플리케이션 컨텍스트 로딩을 검증하는 테스트
package com.sopt.nearby;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.SentryOptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class NearbyApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		assertThat(applicationContext.getBeansOfType(SentryOptions.class)).isEmpty();
	}

}
