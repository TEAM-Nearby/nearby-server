// Nearby 서버 애플리케이션 컨텍스트 로딩을 검증하는 테스트
package com.sopt.nearby;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
	+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
	+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
	+ "org.springframework.modulith.events.jpa.JpaEventPublicationAutoConfiguration")
class NearbyApplicationTests {

	@Test
	void contextLoads() {
	}

}
