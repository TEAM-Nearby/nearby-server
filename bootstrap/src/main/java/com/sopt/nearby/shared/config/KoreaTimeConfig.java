// 한국 표준시 Clock을 애플리케이션 전반에 제공하는 설정 클래스다.
package com.sopt.nearby.shared.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KoreaTimeConfig {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    Clock koreaStandardClock() {
        return Clock.system(KOREA_ZONE);
    }
}
