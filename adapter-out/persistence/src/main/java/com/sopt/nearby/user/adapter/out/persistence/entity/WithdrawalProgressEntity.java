// 공급자별 회원 탈퇴 진행 상태를 저장하는 JPA 엔티티
package com.sopt.nearby.user.adapter.out.persistence.entity;

import com.sopt.nearby.user.domain.model.WithdrawalProgress;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "withdrawal_progress")
public class WithdrawalProgressEntity {
	@Id
	private String id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false)
	private String provider;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WithdrawalProgress.State state;

	protected WithdrawalProgressEntity() {
	}

	public WithdrawalProgressEntity(final WithdrawalProgress progress) {
		this.id = progress.userId() + ":" + progress.provider();
		this.userId = progress.userId();
		this.provider = progress.provider();
		this.state = progress.state();
	}

	public WithdrawalProgress toDomain() {
		return new WithdrawalProgress(userId, provider, state);
	}
}
