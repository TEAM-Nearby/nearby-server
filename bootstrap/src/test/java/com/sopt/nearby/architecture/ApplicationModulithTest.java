// Nearby 애플리케이션 모듈 경계가 Spring Modulith 규칙을 만족하는지 검증하는 테스트
package com.sopt.nearby.architecture;

import com.sopt.nearby.NearbyApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ApplicationModulithTest {

	@Test
	void verifiesApplicationModuleBoundaries() {
		ApplicationModules.of(NearbyApplication.class).verify();
	}
}
