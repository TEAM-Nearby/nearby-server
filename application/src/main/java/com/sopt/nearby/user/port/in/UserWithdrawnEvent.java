// 다른 기능 모듈에 회원 탈퇴 완료 사실을 전달하는 이벤트
package com.sopt.nearby.user.port.in;

public record UserWithdrawnEvent(Long userId) {
}
