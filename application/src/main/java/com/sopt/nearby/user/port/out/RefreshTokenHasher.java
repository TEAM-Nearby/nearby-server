// 원본 리프레시 토큰을 저장소 조회용 해시로 변환하는 포트
package com.sopt.nearby.user.port.out;

public interface RefreshTokenHasher {

	String hash(String refreshToken);
}
