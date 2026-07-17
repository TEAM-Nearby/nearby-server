// 한국 표준시 오프셋을 포함해 LocalDateTime을 HTTP 응답으로 직렬화하는 설정 클래스다.
package com.sopt.nearby.shared.adapter.in.web.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KoreaTimeJacksonConfig {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter RESPONSE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Bean
    public Module koreaTimeModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new KoreaTimeLocalDateTimeSerializer());
        return module;
    }

    private static final class KoreaTimeLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

        @Override
        public void serialize(
                final LocalDateTime value,
                final JsonGenerator generator,
                final SerializerProvider serializers
        ) throws IOException {
            generator.writeString(value.atZone(KOREA_ZONE).format(RESPONSE_FORMAT));
        }
    }
}
