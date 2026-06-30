// 회원 약관 동의 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.user.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_term_agreement")
public class UserTermAgreementEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "term_id", nullable = false)
	private Long termId;

	@Column(name = "agreed_at", nullable = false)
	private LocalDateTime agreedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private UserAccountEntity user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "term_id", insertable = false, updatable = false)
	private TermEntity term;

	protected UserTermAgreementEntity() {
	}

	public UserTermAgreementEntity(final Long id, final Long userId, final Long termId, final LocalDateTime agreedAt) {
		this.id = id;
		this.userId = userId;
		this.termId = termId;
		this.agreedAt = agreedAt;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getTermId() {
		return termId;
	}

	public LocalDateTime getAgreedAt() {
		return agreedAt;
	}
}
