// 소셜 계정 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.user.adapter.out.persistence.repository;

import com.sopt.nearby.user.adapter.out.persistence.entity.SocialAccountEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountJpaRepository extends JpaRepository<SocialAccountEntity, Long> {

	Optional<SocialAccountEntity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
