// 휴대폰 인증 번호를 저장과 비교에 사용할 해시로 변환하는 유틸리티
package com.sopt.nearby.user.application;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class PhoneVerificationCodeHasher {

	private PhoneVerificationCodeHasher() {
	}

	static String hmacSha256(final String value, final String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("휴대폰 인증 번호 HMAC secret이 설정되지 않았습니다.");
		}
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("HMAC-SHA256 알고리즘을 사용할 수 없습니다.", exception);
		}
	}
}
