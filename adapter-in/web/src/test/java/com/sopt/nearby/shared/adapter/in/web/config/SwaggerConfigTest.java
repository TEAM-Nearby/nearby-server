// Swagger OpenAPI 서버 URL이 배포 환경 스킴을 고정하지 않는지 검증하는 테스트
package com.sopt.nearby.shared.adapter.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class SwaggerConfigTest {

	private final SwaggerConfig swaggerConfig = new SwaggerConfig();

	@Test
	void doesNotHardCodeHttpServerUrl() {
		OpenAPI openAPI = swaggerConfig.openAPI();

		assertThat(openAPI.getServers()).isNullOrEmpty();
	}
}
