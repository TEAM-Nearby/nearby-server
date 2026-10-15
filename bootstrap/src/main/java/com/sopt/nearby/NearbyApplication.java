// Nearby 서버 애플리케이션의 Spring Boot 실행 진입점
package com.sopt.nearby;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NearbyApplication {

	public static void main(String[] args) {
		SpringApplication.run(NearbyApplication.class, args);
	}

}
