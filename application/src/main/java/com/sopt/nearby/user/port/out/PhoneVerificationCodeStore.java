// 휴대폰 인증 코드 해시의 TTL 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.user.port.out;

import java.time.Duration;
import java.util.Optional;

public interface PhoneVerificationCodeStore {

	void save(Long phoneVerificationId, String verificationCodeHash, Duration ttl);

	Optional<String> findHash(Long phoneVerificationId);

	void delete(Long phoneVerificationId);
}
