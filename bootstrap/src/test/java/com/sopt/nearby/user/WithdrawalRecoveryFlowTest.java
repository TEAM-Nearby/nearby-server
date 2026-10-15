// 탈퇴 외부 호출의 트랜잭션 분리와 중단 후 복구 흐름을 검증하는 테스트
package com.sopt.nearby.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import com.sopt.nearby.user.application.WithdrawUserCommand;
import com.sopt.nearby.user.config.WithdrawalRecoveryScheduler;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserAccountStatus;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;
import com.sopt.nearby.user.domain.model.WithdrawalProgress;
import com.sopt.nearby.user.exception.SocialAccountUnlinkFailedException;
import com.sopt.nearby.user.port.in.WithdrawUserUseCase;
import com.sopt.nearby.user.port.out.KakaoAccountUnlinker;
import com.sopt.nearby.user.port.out.SocialAccountRepository;
import com.sopt.nearby.user.port.out.UserAccountRepository;
import com.sopt.nearby.user.port.out.WithdrawalProgressRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SpringBootTest
class WithdrawalRecoveryFlowTest {
	@Autowired private UserAccountRepository users;
	@Autowired private SocialAccountRepository socialAccounts;
	@Autowired private WithdrawalProgressRepository progress;
	@Autowired private CompanionProfileRepository companionProfiles;
	@Autowired private WithdrawUserUseCase withdraw;
	@Autowired private WithdrawalRecoveryScheduler scheduler;
	@MockitoBean private KakaoAccountUnlinker kakaoUnlinker;

	@Test
	void reconcilesAmbiguousUnlinkWithoutRepeatingProviderRequest() {
		UserAccount user = users.save(new UserAccount(null, UserRole.USER, UserAccountStatus.ACTIVE,
				null, null, UserOnboardingStatus.STARTED, LocalDateTime.of(2026, 10, 6, 0, 0), null));
		socialAccounts.save(new SocialAccount(null, user.id(), "KAKAO", "kakao-user"));
		AtomicBoolean providerCalledInsideTransaction = new AtomicBoolean(true);
		doAnswer(invocation -> {
			providerCalledInsideTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
			throw new SocialAccountUnlinkFailedException();
		}).when(kakaoUnlinker).unlink(anyString());

		assertThatThrownBy(() -> withdraw.withdraw(new WithdrawUserCommand(user.id())))
				.isInstanceOf(SocialAccountUnlinkFailedException.class);
		assertThat(providerCalledInsideTransaction.get()).isFalse();
		assertThat(users.findById(user.id()).orElseThrow().status()).isEqualTo(UserAccountStatus.WITHDRAWING);
		assertThat(progress.findByUserIdAndProvider(user.id(), "KAKAO").orElseThrow().state())
				.isEqualTo(WithdrawalProgress.State.UNKNOWN);
		assertThat(socialAccounts.findAllByUserId(user.id())).hasSize(1);

		progress.save(new WithdrawalProgress(user.id(), "KAKAO", WithdrawalProgress.State.SUCCEEDED));
		scheduler.resume();

		assertThat(users.findById(user.id()).orElseThrow().status()).isEqualTo(UserAccountStatus.WITHDRAWN);
		assertThat(socialAccounts.findAllByUserId(user.id())).isEmpty();
		verify(kakaoUnlinker).unlink("kakao-user");
		verifyNoMoreInteractions(kakaoUnlinker);
	}

	@Test
	void completesWithdrawalWhenLegacyAnonymizedNicknameIsAlreadyTaken() {
		UserAccount nicknameOwner = users.save(new UserAccount(null, UserRole.USER, UserAccountStatus.ACTIVE,
				null, null, UserOnboardingStatus.STARTED, LocalDateTime.of(2026, 10, 6, 0, 0), null));
		UserAccount withdrawingUser = users.save(new UserAccount(null, UserRole.USER, UserAccountStatus.ACTIVE,
				null, null, UserOnboardingStatus.STARTED, LocalDateTime.of(2026, 10, 6, 0, 0), null));
		companionProfiles.save(profile(nicknameOwner.id(), "탈퇴한 사용자-" + withdrawingUser.id()));
		companionProfiles.save(profile(withdrawingUser.id(), "탈퇴대상-" + withdrawingUser.id()));
		socialAccounts.save(new SocialAccount(null, withdrawingUser.id(), "KAKAO", "kakao-user"));

		withdraw.withdraw(new WithdrawUserCommand(withdrawingUser.id()));

		assertThat(users.findById(withdrawingUser.id()).orElseThrow().status())
				.isEqualTo(UserAccountStatus.WITHDRAWN);
		assertThat(companionProfiles.findByUserId(withdrawingUser.id()).orElseThrow().nickname())
				.isEqualTo("탈퇴한 사용자-" + withdrawingUser.id() + "-탈퇴완료계정");
	}

	private CompanionProfile profile(final Long userId, final String nickname) {
		return new CompanionProfile(null, userId, nickname, UserGender.FEMALE, 2000, null, null,
				new BigDecimal("0.00"), 0, CompanionProfileStatus.ACTIVE);
	}
}
