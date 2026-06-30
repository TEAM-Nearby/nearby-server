// 로그 민감정보 마스킹 규칙을 검증하는 테스트
package com.sopt.nearby.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveLogMaskerTest {

	private final SensitiveLogMasker sensitiveLogMasker = new SensitiveLogMasker();

	@Test
	void masksBearerTokenPhoneNumberAndPasswordValue() {
		String masked = sensitiveLogMasker.mask(
				"Authorization: Bearer abc.def.ghi, phone=010-1234-5678, password=plain-password"
		);

		assertThat(masked).doesNotContain("abc.def.ghi");
		assertThat(masked).doesNotContain("010-1234-5678");
		assertThat(masked).doesNotContain("plain-password");
		assertThat(masked).contains("Bearer ***");
		assertThat(masked).contains("010-****-5678");
		assertThat(masked).contains("password=***");
	}
}
