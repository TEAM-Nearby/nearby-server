// 휴대폰 인증 번호를 저장과 비교에 사용할 해시로 변환하는 유틸리티
package com.sopt.nearby.user.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class PhoneVerificationCodeHasher {

	private PhoneVerificationCodeHasher() {
	}

	static String sha256(final String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
		}
	}
}
