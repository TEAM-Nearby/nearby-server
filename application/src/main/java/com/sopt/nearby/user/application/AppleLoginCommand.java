// 애플 로그인 요청에 필요한 검증 토큰 값을 담는 커맨드
package com.sopt.nearby.user.application;

public record AppleLoginCommand(
        String idToken,
        String nonce
) {
}