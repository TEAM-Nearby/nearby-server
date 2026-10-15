// Nearby 애플리케이션의 내장 웹서버 기동을 검증하는 테스트
package com.sopt.nearby;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NearbyApplicationWebServerTest {

	@Test
	void startsEmbeddedWebServer() {
	}
}
