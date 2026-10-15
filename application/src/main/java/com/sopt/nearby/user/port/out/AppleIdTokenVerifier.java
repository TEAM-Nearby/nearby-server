// 애플 ID 토큰 검증을 외부 보안 어뎁터에 위임하는 포트
package com.sopt.nearby.user.port.out;

import com.sopt.nearby.user.application.VerifiedUser;

public interface AppleIdTokenVerifier {
    VerifiedUser verify(String idToken, String nonce);
}
