// 요청 단위 추적에 사용할 식별자를 생성하는 컴포넌트
package com.sopt.nearby.logging;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RequestIdGenerator {

	public String generate() {
		return UUID.randomUUID().toString();
	}
}
