// 외부 푸시 공급자에 전달할 기술 중립적인 메시지를 표현한다.
package com.sopt.nearby.companion.port.out;

import java.util.Map;

public record PushMessage(
        Long deliveryId,
        String token,
        String title,
        String body,
        Map<String, String> data
) {
}
