// 외부 푸시 공급자 호출을 추상화하는 포트다.
package com.sopt.nearby.companion.port.out;

import java.util.List;

public interface PushSender {

    List<PushDeliveryResult> send(List<PushMessage> messages);
}
