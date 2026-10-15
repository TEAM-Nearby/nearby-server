// UTC 기준 Clock을 애플리케이션 전반에 제공한다.
package com.sopt.nearby.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UtcTimeConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
