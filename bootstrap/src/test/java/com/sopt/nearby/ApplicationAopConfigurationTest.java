// 애플리케이션 AOP 프록시 기본 방식을 검증하는 테스트
package com.sopt.nearby;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ApplicationAopConfigurationTest {

	@Test
	void usesJdkInterfaceProxiesForServletFilters() throws IOException {
		List<PropertySource<?>> propertySources = new YamlPropertySourceLoader()
				.load("application", new ClassPathResource("application.yaml"));

		assertThat(propertySources.get(0).getProperty("spring.aop.proxy-target-class")).isEqualTo(false);
	}
}
