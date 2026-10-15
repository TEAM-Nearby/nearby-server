// 카카오 계정 연동 해제를 추상화하는 포트
package com.sopt.nearby.user.port.out;

public interface KakaoAccountUnlinker {

	void unlink(String providerUserId);
}
