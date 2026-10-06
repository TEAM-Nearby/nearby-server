// 중단된 회원 탈퇴 중 안전하게 재개할 수 있는 작업을 주기적으로 처리하는 스케줄러
package com.sopt.nearby.user.config;

import com.sopt.nearby.user.application.WithdrawUserCommand;
import com.sopt.nearby.user.port.in.WithdrawUserUseCase;
import com.sopt.nearby.user.port.out.WithdrawalProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class WithdrawalRecoveryScheduler {
	private static final Logger log = LoggerFactory.getLogger(WithdrawalRecoveryScheduler.class);
	private final WithdrawalProgressRepository progressRepository;
	private final WithdrawUserUseCase withdrawUserUseCase;

	public WithdrawalRecoveryScheduler(final WithdrawalProgressRepository progressRepository,
			final WithdrawUserUseCase withdrawUserUseCase) {
		this.progressRepository = progressRepository;
		this.withdrawUserUseCase = withdrawUserUseCase;
	}

	@Scheduled(fixedDelayString = "${nearby.withdrawal.recovery-delay-ms:60000}",
			initialDelayString = "${nearby.withdrawal.recovery-delay-ms:60000}")
	public void resume() {
		for (Long userId : progressRepository.findResumableUserIds(50)) {
			try {
				withdrawUserUseCase.withdraw(new WithdrawUserCommand(userId));
			} catch (RuntimeException exception) {
				log.error("중단된 회원 탈퇴 재개에 실패했습니다. userId={}", userId, exception);
			}
		}
	}
}
