// Supabase 전용 Spring 설정 파일의 필수 속성을 검증하는 테스트
package com.sopt.nearby;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class SupabaseProfileConfigurationTest {

	@Test
	void loadsSupabaseDatasourceAndMigrationSettings() throws IOException {
		ClassPathResource resource = new ClassPathResource("application-supabase.yaml");

		assertThat(resource.exists()).isTrue();

		List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load("supabase", resource);
		PropertySource<?> properties = propertySources.get(0);

		assertThat(properties.getProperty("spring.datasource.url")).isEqualTo("${SPRING_DATASOURCE_URL}");
		assertThat(properties.getProperty("spring.datasource.username")).isEqualTo("${SPRING_DATASOURCE_USERNAME}");
		assertThat(properties.getProperty("spring.datasource.password")).isEqualTo("${SPRING_DATASOURCE_PASSWORD}");
		assertThat(properties.getProperty("spring.datasource.hikari.maximum-pool-size"))
				.isEqualTo("${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:5}");
		assertThat(properties.getProperty("spring.datasource.hikari.connection-init-sql"))
				.isEqualTo("SET TIME ZONE 'Asia/Seoul'");
		assertThat(properties.getProperty("spring.data.redis.host")).isEqualTo("${SPRING_DATA_REDIS_HOST}");
		assertThat(properties.getProperty("spring.data.redis.port"))
				.isEqualTo("${SPRING_DATA_REDIS_PORT:6379}");
		assertThat(properties.getProperty("spring.data.redis.ssl.enabled"))
				.isEqualTo("${SPRING_DATA_REDIS_SSL_ENABLED:true}");
		assertThat(properties.getProperty("spring.data.redis.url")).isNull();
		assertThat(properties.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
		assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo(true);
		assertThat(properties.getProperty("management.endpoint.health.show-components")).isEqualTo("always");
		assertThat(properties.getProperty("management.health.redis.enabled")).isEqualTo(true);
	}
}
