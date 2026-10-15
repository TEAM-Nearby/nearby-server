// 회원 탈퇴 대상 사용자 식별자를 담는 커맨드
package com.sopt.nearby.user.application;

public record WithdrawUserCommand(Long userId) {
}
