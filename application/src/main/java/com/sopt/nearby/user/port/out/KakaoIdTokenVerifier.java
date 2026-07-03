// 카카오 ID 토큰 검증을 외부 보안 어댑터에 위임하는 포트
package com.sopt.nearby.user.port.out;

import com.sopt.nearby.user.application.VerifiedKakaoUser;

public interface KakaoIdTokenVerifier {

	VerifiedKakaoUser verify(String idToken, String nonce);
}
