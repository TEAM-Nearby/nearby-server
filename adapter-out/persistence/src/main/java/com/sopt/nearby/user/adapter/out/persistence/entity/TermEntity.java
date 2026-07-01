// 약관 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.user.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "term")
public class TermEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "term_key", nullable = false)
	private String termKey;

	@Column(nullable = false)
	private String version;

	@Column(nullable = false)
	private boolean required;

	protected TermEntity() {
	}

	public TermEntity(final Long id, final String termKey, final String version, final boolean required) {
		this.id = id;
		this.termKey = termKey;
		this.version = version;
		this.required = required;
	}

	public Long getId() {
		return id;
	}

	public String getTermKey() {
		return termKey;
	}

	public String getVersion() {
		return version;
	}

	public boolean isRequired() {
		return required;
	}
}
