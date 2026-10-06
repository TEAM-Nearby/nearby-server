// 회원 탈퇴의 공급자별 진행 상태를 저장하고 조회하는 포트
package com.sopt.nearby.user.port.out;

import com.sopt.nearby.user.domain.model.WithdrawalProgress;
import java.util.List;
import java.util.Optional;

public interface WithdrawalProgressRepository {
	WithdrawalProgress save(WithdrawalProgress progress);

	Optional<WithdrawalProgress> findByUserIdAndProvider(Long userId, String provider);

	List<WithdrawalProgress> findAllByUserId(Long userId);

	List<Long> findResumableUserIds(int limit);

	void deleteByUserId(Long userId);
}
