// 회원 도메인 모델과 JPA 엔티티 사이의 필드 매핑을 담당하는 클래스
package com.sopt.nearby.user.adapter.out.persistence.mapper;

import com.sopt.nearby.user.adapter.out.persistence.entity.EmergencyContactEntity;
import com.sopt.nearby.user.adapter.out.persistence.entity.PhoneVerificationEntity;
import com.sopt.nearby.user.adapter.out.persistence.entity.RefreshTokenEntity;
import com.sopt.nearby.user.adapter.out.persistence.entity.SocialAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.entity.TermEntity;
import com.sopt.nearby.user.adapter.out.persistence.entity.UserAccountEntity;
import com.sopt.nearby.user.adapter.out.persistence.entity.UserTermAgreementEntity;
import com.sopt.nearby.user.domain.model.EmergencyContact;
import com.sopt.nearby.user.domain.model.PhoneVerification;
import com.sopt.nearby.user.domain.model.RefreshToken;
import com.sopt.nearby.user.domain.model.SocialAccount;
import com.sopt.nearby.user.domain.model.Term;
import com.sopt.nearby.user.domain.model.UserAccount;
import com.sopt.nearby.user.domain.model.UserTermAgreement;

public final class UserPersistenceMapper {

	private UserPersistenceMapper() {
	}

	public static UserAccountEntity toEntity(final UserAccount model) {
		return new UserAccountEntity(
				model.id(),
				model.role(),
				model.status(),
				model.phoneNumber(),
				model.phoneVerifiedAt(),
				model.onboardingStatus(),
				model.createdAt(),
				model.deletedAt()
		);
	}

	public static UserAccount toDomain(final UserAccountEntity entity) {
		return new UserAccount(
				entity.getId(),
				entity.getRole(),
				entity.getStatus(),
				entity.getPhoneNumber(),
				entity.getPhoneVerifiedAt(),
				entity.getOnboardingStatus(),
				entity.getCreatedAt(),
				entity.getDeletedAt()
		);
	}

	public static TermEntity toEntity(final Term model) {
		return new TermEntity(model.id(), model.termKey(), model.version(), model.required());
	}

	public static Term toDomain(final TermEntity entity) {
		return new Term(entity.getId(), entity.getTermKey(), entity.getVersion(), entity.isRequired());
	}

	public static UserTermAgreementEntity toEntity(final UserTermAgreement model) {
		return new UserTermAgreementEntity(model.id(), model.userId(), model.termId(), model.agreedAt());
	}

	public static UserTermAgreement toDomain(final UserTermAgreementEntity entity) {
		return new UserTermAgreement(entity.getId(), entity.getUserId(), entity.getTermId(), entity.getAgreedAt());
	}

	public static SocialAccountEntity toEntity(final SocialAccount model) {
		return new SocialAccountEntity(model.id(), model.userId(), model.provider(), model.providerUserId());
	}

	public static SocialAccount toDomain(final SocialAccountEntity entity) {
		return new SocialAccount(entity.getId(), entity.getUserId(), entity.getProvider(), entity.getProviderUserId());
	}

	public static RefreshTokenEntity toEntity(final RefreshToken model) {
		return new RefreshTokenEntity(
				model.id(),
				model.userId(),
				model.tokenHash(),
				model.expiresAt(),
				model.revokedAt()
		);
	}

	public static RefreshToken toDomain(final RefreshTokenEntity entity) {
		return new RefreshToken(
				entity.getId(),
				entity.getUserId(),
				entity.getTokenHash(),
				entity.getExpiresAt(),
				entity.getRevokedAt()
		);
	}

	public static PhoneVerificationEntity toEntity(final PhoneVerification model) {
		return new PhoneVerificationEntity(
				model.id(),
				model.userId(),
				model.phoneNumber(),
				model.carrier(),
				model.verificationCodeHash(),
				model.status(),
				model.expiresAt(),
				model.verifiedAt()
		);
	}

	public static PhoneVerification toDomain(final PhoneVerificationEntity entity) {
		return new PhoneVerification(
				entity.getId(),
				entity.getUserId(),
				entity.getPhoneNumber(),
				entity.getCarrier(),
				entity.getVerificationCodeHash(),
				entity.getStatus(),
				entity.getExpiresAt(),
				entity.getVerifiedAt()
		);
	}

	public static EmergencyContactEntity toEntity(final EmergencyContact model) {
		return new EmergencyContactEntity(model.id(), model.userId(), model.name(), model.phoneNumber());
	}

	public static EmergencyContact toDomain(final EmergencyContactEntity entity) {
		return new EmergencyContact(entity.getId(), entity.getUserId(), entity.getName(), entity.getPhoneNumber());
	}
}
