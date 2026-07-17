// 한국 표준시 LocalDateTime HTTP 응답 직렬화를 검증한다.
package com.sopt.nearby.shared.adapter.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KoreaTimeJacksonConfigTest {

    @Test
    void serializesLocalDateTimeWithKoreaOffset() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new KoreaTimeJacksonConfig().koreaTimeModule());

        String json = objectMapper.writeValueAsString(Map.of(
                "createdAt", LocalDateTime.of(2026, 7, 17, 9, 0)
        ));

        assertThat(json).contains("2026-07-17T09:00:00+09:00");
    }
}
