// 요청 식별자 생성 규칙을 검증하는 테스트
package com.sopt.nearby.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestIdGeneratorTest {

	private final RequestIdGenerator requestIdGenerator = new RequestIdGenerator();

	@Test
	void generatesUuidRequestId() {
		String requestId = requestIdGenerator.generate();

		assertThat(requestId).isNotBlank();
		assertThatCode(() -> UUID.fromString(requestId)).doesNotThrowAnyException();
	}
}
