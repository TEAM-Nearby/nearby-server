// 동행 푸시 문구에 사용할 수신자 닉네임과 장소명을 조회하는 포트다.
package com.sopt.nearby.companion.port.out;

import java.util.Optional;

public interface CompanionPushContentQueryPort {

    Optional<String> findRecipientNickname(Long recipientUserId);

    Optional<String> findPlaceNameByApplicationId(Long applicationId);
}
