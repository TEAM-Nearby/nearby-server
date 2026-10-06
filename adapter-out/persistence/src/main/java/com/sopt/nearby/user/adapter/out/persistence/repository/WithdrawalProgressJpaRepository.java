// 공급자별 회원 탈퇴 진행 상태의 JPA 조회를 정의하는 저장소
package com.sopt.nearby.user.adapter.out.persistence.repository;

import com.sopt.nearby.user.adapter.out.persistence.entity.WithdrawalProgressEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WithdrawalProgressJpaRepository extends JpaRepository<WithdrawalProgressEntity, String> {
	List<WithdrawalProgressEntity> findAllByUserId(Long userId);

	@Query(value = "select p.user_id from withdrawal_progress p join user_account u on u.id = p.user_id "
			+ "where u.status = 'WITHDRAWING' group by p.user_id "
			+ "having sum(case when p.state in ('IN_FLIGHT', 'UNKNOWN') then 1 else 0 end) = 0 "
			+ "limit :limit", nativeQuery = true)
	List<Long> findResumableUserIds(@Param("limit") int limit);

	void deleteByUserId(Long userId);
}
