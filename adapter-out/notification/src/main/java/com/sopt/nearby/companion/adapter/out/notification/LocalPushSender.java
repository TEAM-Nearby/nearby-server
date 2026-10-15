// 개발 환경에서 푸시 발송을 로그로 대체하는 어댑터다.
package com.sopt.nearby.companion.adapter.out.notification;

import com.sopt.nearby.companion.port.out.PushDeliveryResult;
import com.sopt.nearby.companion.port.out.PushMessage;
import com.sopt.nearby.companion.port.out.PushSender;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev", "test"})
public class LocalPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LocalPushSender.class);

    @Override
    public List<PushDeliveryResult> send(final List<PushMessage> messages) {
        return messages.stream()
                .map(message -> {
                    log.info(
                            "푸시 발송을 로컬 처리했습니다. deliveryId={}, token={}, title={}",
                            message.deliveryId(),
                            mask(message.token()),
                            message.title()
                    );
                    return new PushDeliveryResult(
                            message.deliveryId(),
                            PushDeliveryResult.Outcome.SENT,
                            "local-" + message.deliveryId(),
                            null,
                            null
                    );
                })
                .toList();
    }

    private String mask(final String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
