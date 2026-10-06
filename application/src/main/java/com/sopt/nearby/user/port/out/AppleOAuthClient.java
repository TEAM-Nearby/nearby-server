// Apple 인증 코드 교환과 계정 연동 해제를 추상화하는 포트
package com.sopt.nearby.user.port.out;

public interface AppleOAuthClient {

	Tokens exchangeAuthorizationCode(String authorizationCode);

	void revoke(String refreshToken);

	record Tokens(String refreshToken, String idToken) {
	}
}
